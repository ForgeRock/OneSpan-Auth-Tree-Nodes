/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */
package com.os.tid.forgerock.openam.nodes;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.models.UserRegisterOutputEx;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.SslUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

/**
 * This node invokes the User Register/Unregister API, which validates and processes the registration/unregistration of a user.
 */
@Node.Metadata( outcomeProvider = OS_Auth_UserRegisterNode.OSTIDUserRegisterOutcomeProvider.class,
                configClass = OS_Auth_UserRegisterNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_UserRegisterNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_UserRegisterNode";
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_UserRegisterNode.class);
    private final OS_Auth_UserRegisterNode.Config config;
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth User Register]" + OSAuthNodePlugin.logAppender;

    /**
     * Configuration for the OneSpan Auth User Register Node.
     */
    public interface Config {
        /**
         * Domain wherein to search for user accounts.
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default String domain() {
            return Constants.OSTID_DEFAULT_DOMAIN;
        }
        
        /**
         * Input payload object type.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default ObjectType objectType() { return ObjectType.IAA; }

        /**
         * If the node functions as user registration or unregistration.
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default NodeFunction nodeFunction() {
            return NodeFunction.UserRegister;
        }

        /**
         * The key name in Shared State which represents the IAA/OCA username
         */
        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         * Indicates if the authenticator assigned to the user must be activated using online or offline multi-device licensing (MDL) activation.
         */
        @Attribute(order = 500, validators = RequiredValueValidator.class)
        default ActivationType activationType() {
            return ActivationType.onlineMDL;
        }

        /**
         * Configurable attributes in request JSON payload
         */
        @Attribute(order = 600)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

        /**
         * Timeout in seconds.
         */
        @Attribute(order = 700, validators = RequiredValueValidator.class)
        default int activationTokenExpiry() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }
    }

    @Inject
    public OS_Auth_UserRegisterNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
    	try {
	        logger.debug(loggerPrefix + "OS_Auth_UserRegisterNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
            String customUrl = serviceConfig.customUrl().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        JsonValue cddcJsonJsonValue = sharedState.get(Constants.OSTID_CDDC_JSON);
	        JsonValue cddcHashJsonValue = sharedState.get(Constants.OSTID_CDDC_HASH);
	        JsonValue cddcIpJsonValue = sharedState.get(Constants.OSTID_CDDC_IP);
	
	        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE, config.userNameInSharedData());
	
	        boolean allOptionalFieldsIncluded = true;
	        StringBuilder optionalAttributesStringBuilder = new StringBuilder(1000);
	        Map<String, String> optionalAttributesMap = config.optionalAttributes();
	        for (Map.Entry<String, String> entrySet : optionalAttributesMap.entrySet()) {
	        	JsonValue jsonValue;
	        	if(Constants.OSTID_STATIC_PASSWORD.equalsIgnoreCase(entrySet.getKey())) {
	        		jsonValue = sharedState.get(entrySet.getValue());
	        	}else {
	        		jsonValue = sharedState.get(entrySet.getValue());
	        	} 
	            if (jsonValue.isString()) {
	                optionalAttributesStringBuilder.append("\"").append(entrySet.getKey()).append("\":\"").append(jsonValue.asString()).append("\",");
	            } else {
	                allOptionalFieldsIncluded = false;
	            }
	        }
	
	        if (!allOptionalFieldsIncluded || CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                        usernameJsonValue,
	                        cddcJsonJsonValue,
	                        cddcHashJsonValue,
	                        cddcIpJsonValue
	                ))
	        ) {  //missing data
	            throw new NodeProcessException("Oopts, there are missing data for OneSpan Auth User Register Process!");
	        } 
	        
            String APIUrl = config.nodeFunction() == NodeFunction.UserRegister ?
                    Constants.OSTID_API_ADAPTIVE_USER_REGISTER
                    :
                    String.format(Constants.OSTID_API_ADAPTIVE_USER_UNREGISTER,usernameJsonValue.asString(),config.domain());
            //param 1
            String objectType = "";
            switch(config.objectType()) {
                case IAA:
                    objectType = config.nodeFunction() == NodeFunction.UserRegister ? "AdaptiveRegisterUserInput" : "AdaptiveUnregisterUserInput";
                    break;
                case OCA:
                    objectType = config.nodeFunction() == NodeFunction.UserRegister ? "RegisterUserInputEx" : "UnregisterUserInputEx";
                    break;
            }
            //param 7
            String applicationRef = config.objectType() == ObjectType.IAA ? String.format(Constants.OSTID_JSON_ADAPTIVE_APPLICATIONREF, serviceConfig.applicationRef()) : "";
            //param 8
            String sessionId = sharedState.get(Constants.OSTID_SESSIONID).isString() ? sharedState.get(Constants.OSTID_SESSIONID).asString() : StringUtils.stringToHex(UUID.randomUUID().toString());
            String sessionIdJSON = config.objectType() == ObjectType.IAA ? String.format(Constants.OSTID_JSON_ADAPTIVE_SESSIONID, sessionId) : "";
            //param 9
            String relationshipRef = sharedState.get("relationshipRef").isString() ? sharedState.get("relationshipRef").asString():usernameJsonValue.asString();
            relationshipRef = config.objectType() == ObjectType.IAA ? String.format(Constants.OSTID_JSON_ADAPTIVE_USER_REGISTER_RELATIONSHIPREF, relationshipRef) : "";
            //param 10
            String activationType = String.format(Constants.OSTID_JSON_ADAPTIVE_USER_REGISTER_ACTIVATIONTYPE, config.activationType().name());

            String userRegisterJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_USER_REGISTER,
                    objectType,                                                                      //param1
                    usernameJsonValue.asString(),                                                    //param2
                    cddcIpJsonValue.asString(),                                                      //param3
                    cddcHashJsonValue.asString(),                                                    //param4
                    cddcJsonJsonValue.asString(),                                                    //param5
                    applicationRef,                                                                  //param6
                    sessionIdJSON,                                                                   //param7
                    relationshipRef,                                                                 //param8
                    activationType,                                                                  //param9
                    optionalAttributesStringBuilder.toString()                                       //param10
            );
            logger.debug(loggerPrefix + "OS_Auth_UserRegisterNode userRegisterJSON:" + userRegisterJSON);

            HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + APIUrl, userRegisterJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
            JSONObject responseJSON = httpEntity.getResponseJSON();

            if (httpEntity.isSuccess()) {
                UserRegisterOutputEx userRegisterOutputEx = JSON.toJavaObject(responseJSON, UserRegisterOutputEx.class);
                String activationCode = userRegisterOutputEx.getActivationPassword();
                if (config.nodeFunction() == NodeFunction.UserRegister && config.activationType() == ActivationType.onlineMDL) {
                    //"02;user01211;111;duoliang11071-mailin;3zE6RNH5;duoliang11071-mailin"
                	String userProfile = sharedState.get(Constants.OSTID_USERPROFILE_IN_SHARED_STATE).isNull() ? "0" : sharedState.get(Constants.OSTID_USERPROFILE_IN_SHARED_STATE).asString();            	
                    String crontoValueRaw = String.format(Constants.OSTID_CRONTO_FORMULA,
                            Constants.OSTID_API_VERSION,                        //param1
                            usernameJsonValue.asString(),                       //param2
                            config.domain(),                                    //param3
                            activationCode,                                     //param4
                            tenantName,                                         //param5
                            userProfile                                         //param6
                    );
                    String crontoValueHex = StringUtils.stringToHex(crontoValueRaw);

                    sharedState.put(Constants.OSTID_SESSIONID, sessionId);
                    sharedState.put(Constants.OSTID_ACTIVATION_CODE, activationCode);
                    sharedState.put(Constants.OSTID_ACTIVATION_CODE2, activationCode);
                    sharedState.put(Constants.OSTID_CRONTO_MSG, crontoValueHex);
                    sharedState.put(Constants.OSTID_DIGI_SERIAL, userRegisterOutputEx.getSerialNumber());
                    sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.activationTokenExpiry()));
                } else if (config.nodeFunction() == NodeFunction.UserRegister && config.activationType() == ActivationType.offlineMDL) {
                	sharedState.put(Constants.OSTID_SESSIONID, sessionId);
                	sharedState.put(Constants.OSTID_ACTIVATION_CODE, activationCode);
                	sharedState.put(Constants.OSTID_ACTIVATION_CODE2, activationCode);
                	sharedState.put(Constants.OSTID_CRONTO_MSG, activationCode);
                	sharedState.put(Constants.OSTID_DIGI_SERIAL, userRegisterOutputEx.getSerialNumber());
                	sharedState.put(Constants.OSTID_REGISTRATION_ID, userRegisterOutputEx.getRegistrationID());
                	sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.activationTokenExpiry()));
                } else if (config.nodeFunction() == NodeFunction.UserUnregister) {
                	sharedState.put(Constants.OSTID_SESSIONID, sessionId);
                }
                return goTo(UserRegisterOutcome.Success).build();
            } else {
                String log_correction_id = httpEntity.getLog_correlation_id();
                String message = responseJSON.getString("message");
                String requestJSON = "POST " + StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + APIUrl + " : " + userRegisterJSON;

                if (Stream.of(log_correction_id, message).anyMatch(Objects::isNull)) {
                    throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(responseJSON));
                } else {
                    JSONArray validationErrors = responseJSON.getJSONArray("validationErrors");
                    if(validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null){
                    	String errorMsgNoRetCodeWithValidation = StringUtils.getErrorMsgNoRetCodeWithValidation(message,log_correction_id,validationErrors.getJSONObject(0).getString("message"),requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithValidation);
                    }else{
                    	String errorMsgNoRetCodeWithoutValidation = StringUtils.getErrorMsgNoRetCodeWithoutValidation(message,log_correction_id,requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithoutValidation);
                    }
                }
            }
    	}catch (Exception ex) {
	   		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_UserRegisterNode Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan User Register process: " + ex.getMessage());
			return goTo(UserRegisterOutcome.Error).build();	
	    }
    }

    public enum ObjectType {
        IAA, OCA
    }
    public enum ActivationType {
        offlineMDL, onlineMDL, fido
    }

    public enum NodeFunction {
        UserRegister, UserUnregister
    }

    public enum UserRegisterOutcome {
        Success, Error
    }

    private Action.ActionBuilder goTo(OS_Auth_UserRegisterNode.UserRegisterOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDUserRegisterOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_UserRegisterNode.BUNDLE,
                    OS_Auth_UserRegisterNode.OSTIDUserRegisterOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Auth_UserRegisterNode.UserRegisterOutcome.Success.name(), bundle.getString("successOutcome")),
                    new Outcome(OS_Auth_UserRegisterNode.UserRegisterOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
