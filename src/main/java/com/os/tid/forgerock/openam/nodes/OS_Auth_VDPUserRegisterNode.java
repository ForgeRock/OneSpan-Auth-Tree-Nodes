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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.models.UserRegisterOutputEx;
import com.os.tid.forgerock.openam.nodes.OS_Auth_ActivateDeviceNode.OSTIDActivateDeviceOutcome;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Stream;

/**
 * This node invokes the User Register/Unregister API, which validates and processes the registration/unregistration of a user.
 */
@Node.Metadata( outcomeProvider = OS_Auth_VDPUserRegisterNode.OSVdpUserRegisterOutcomeProvider.class,
                configClass = OS_Auth_VDPUserRegisterNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_VDPUserRegisterNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_VDPUserRegisterNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OS_Auth_VDPUserRegisterNode.Config config;
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth VDP User Register][Marketplace] ";

    /**
     * Configuration for the OneSpan Auth User Register Node.
     */
    public interface Config {
        /**
         * The key name in Shared State which represents the IAA/OCA username
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         * Indicates if the authenticator assigned to the user must be activated using online or offline multi-device licensing (MDL) activation.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default DeliveryMethod vdpDeliveryMethod() {
            return DeliveryMethod.Email;
        }

        /**
         * Configurable attributes in request JSON payload
         */
        @Attribute(order = 300)
        default Map<String, String> optionalAttributes() {
            return ImmutableMap.of("emailAddress", "emailAddress");
        }

    }

    @Inject
    public OS_Auth_VDPUserRegisterNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_VDPUserRegisterNode started");
	        JsonValue sharedState = context.sharedState;
	        JsonValue transientState = context.transientState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE, config.userNameInSharedData());
	
	        boolean allOptionalFieldsIncluded = true;
	        StringBuilder optionalAttributesStringBuilder = new StringBuilder(1000);
	        Map<String, String> optionalAttributesMap = config.optionalAttributes();
	        for (Map.Entry<String, String> entrySet : optionalAttributesMap.entrySet()) {
	        	JsonValue jsonValue;
	        	if(Constants.OSTID_STATIC_PASSWORD.equalsIgnoreCase(entrySet.getKey())) {
	        		jsonValue = transientState.get(entrySet.getValue());
	        	}else {
	        		jsonValue = sharedState.get(entrySet.getValue());
	        	}
	        	
	            if (jsonValue.isString()) {
	                optionalAttributesStringBuilder.append("\"").append(entrySet.getKey()).append("\":\"").append(jsonValue.asString()).append("\",");
	            } else {
	                allOptionalFieldsIncluded = false;
	            }
	        }
	
	        if (!allOptionalFieldsIncluded || CollectionsUtils.hasAnyNullValues(ImmutableList.of(usernameJsonValue))
	        ) {  //missing data
	            throw new NodeProcessException("Oopts, there are missing data for OneSpan Auth VDP User Register Process!");
	        } 
	        
            String APIUrl = String.format(Constants.OSTID_API_VDP_USER_REGISTER,usernameJsonValue.asString(),tenantName);

            //step1: GET /v1/users/user1@duoliang-onespan
            HttpEntity getUserHttpEntity = RestUtils.doGet(StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl);

            
            //if exist: PATCH /v1/users/user1@duoliang-onespan
            if(getUserHttpEntity.isSuccess()) {
                String vdpUserRegisterJSON = String.format(Constants.OSTID_JSON_VDP_USER_REGISTER,
                        optionalAttributesStringBuilder.toString(),                              //param1
                        config.vdpDeliveryMethod().name()                                        //param2
                );
                logger.debug(loggerPrefix + "OS_Auth_VDPUserRegisterNode vdpUserRegisterJSON:" + vdpUserRegisterJSON);

                HttpEntity httpEntity = RestUtils.doPatchJSON(StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl, vdpUserRegisterJSON);
                JSONObject responseJSON = httpEntity.getResponseJSON();

                if (httpEntity.isSuccess()) {
                    return goTo(VDPUserRegisterOutcome.Success)
                            .replaceSharedState(sharedState)
                            .replaceTransientState(transientState)
                            .build();
                } else {
                    String log_correction_id = httpEntity.getLog_correlation_id();
                    String message = responseJSON.getString("message");
                    String requestJSON = "PUT " + StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl + " : " + vdpUserRegisterJSON;

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
            //if the user doesn't exist: PUT /v1/users/user1@duoliang-onespan
            }else {
                String vdpUserRegisterJSON = String.format(Constants.OSTID_JSON_VDP_USER_REGISTER,
                        optionalAttributesStringBuilder.toString(),                              //param1
                        config.vdpDeliveryMethod().name()                                        //param2
                );
                logger.debug(loggerPrefix + "OS_Auth_VDPUserRegisterNode vdpUserRegisterJSON:" + vdpUserRegisterJSON);

                HttpEntity httpEntity = RestUtils.doPutJSON(StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl, vdpUserRegisterJSON);
                JSONObject responseJSON = httpEntity.getResponseJSON();

                if (httpEntity.isSuccess()) {
                    return goTo(VDPUserRegisterOutcome.Success)
                            .replaceSharedState(sharedState)
                            .replaceTransientState(transientState)
                            .build();
                } else {
                    String log_correction_id = httpEntity.getLog_correlation_id();
                    String message = responseJSON.getString("message");
                    String requestJSON = "PUT " + StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl + " : " + vdpUserRegisterJSON;

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
            }
    	}catch (Exception ex) {
    		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
//			context.getStateFor(this).putShared("OS_Auth_VDPUserRegisterNode Exception", new Date() + ": " + stackTrace)
//									 .putShared(Constants.OSTID_ERROR_MESSAGE, "OneSpan VDP User Register process: " + stackTrace);
			JsonValue sharedState = context.sharedState;
		    JsonValue transientState = context.transientState;
			sharedState.put("OS_Auth_VDPUserRegisterNode Exception", new Date() + ": " + ex.getMessage());
			sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan VDP User Register process: " + ex.getMessage());
			return goTo(VDPUserRegisterOutcome.Error)
                     .replaceSharedState(sharedState)
                     .replaceTransientState(transientState)
                     .build();	    
		 }
    }

   
    public enum DeliveryMethod {
    	Default, SMS, Email, Voice
    }

 

    public enum VDPUserRegisterOutcome {
        Success, Error
    }

    private Action.ActionBuilder goTo(OS_Auth_VDPUserRegisterNode.VDPUserRegisterOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSVdpUserRegisterOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_VDPUserRegisterNode.BUNDLE,
                    OS_Auth_VDPUserRegisterNode.OSVdpUserRegisterOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Auth_VDPUserRegisterNode.VDPUserRegisterOutcome.Success.name(), bundle.getString("successOutcome")),
                    new Outcome(OS_Auth_VDPUserRegisterNode.VDPUserRegisterOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
