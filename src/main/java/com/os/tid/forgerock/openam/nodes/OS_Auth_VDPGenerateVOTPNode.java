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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.SslUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

/**
 * This node invokes the Generate Challenge API, which generates a random challenge.
 */
@Node.Metadata( outcomeProvider = OS_Auth_VDPGenerateVOTPNode.OSVdpGenerateVOTPOutcomeProvider.class,
                configClass = OS_Auth_VDPGenerateVOTPNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_VDPGenerateVOTPNode implements Node {
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_VDPGenerateVOTPNode.class);
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_VDPGenerateVOTPNode";
    private final OSConfigurationsService serviceConfig;
    private final OS_Auth_VDPGenerateVOTPNode.Config config;
    private static final String loggerPrefix = "[OneSpan Auth VDP Generate VOTP]" + OSAuthNodePlugin.logAppender;

    /**
     * Configuration for the OS Auth Generate Challenge Node.
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
         * The key name in Shared State which represents the OCA username
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }
        
        /**
         * Indicates if the authenticator assigned to the user must be activated using online or offline multi-device licensing (MDL) activation.
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default DeliveryMethod vdpDeliveryMethod() {
            return DeliveryMethod.Email;
        }
        
        /**
         * Configurable attributes in request JSON payload
         */
        @Attribute(order = 400)
        default Map<String, String> optionalAttributes() {
            return ImmutableMap.of("emailAddress", "emailAddress");
        }
      
    }

    @Inject
    public OS_Auth_VDPGenerateVOTPNode(@Assisted OS_Auth_VDPGenerateVOTPNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_VDPGenerateVOTPNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE, config.userNameInSharedData());
	
	        boolean allOptionalFieldsIncluded = true;
	        StringBuilder optionalAttributesStringBuilder = new StringBuilder(1000);
	        Map<String, String> optionalAttributesMap = config.optionalAttributes();
	        for (Map.Entry<String, String> entrySet : optionalAttributesMap.entrySet()) {
	        	JsonValue jsonValue;
        		jsonValue = sharedState.get(entrySet.getValue());
	        	
	            if (jsonValue.isString()) {
	                optionalAttributesStringBuilder.append("\"").append(entrySet.getKey()).append("\":\"").append(jsonValue.asString()).append("\",");
	            } else {
	                allOptionalFieldsIncluded = false;
	            }
	        }
	
	        if (!allOptionalFieldsIncluded || CollectionsUtils.hasAnyNullValues(ImmutableList.of(usernameJsonValue))
	        ) {  //missing data
	            throw new NodeProcessException("Oopts, there are missing data for OneSpan Auth Generate VOTP Process!");
	        } 
	        
    		
	        //API1: GET /v1/users/duotest2305011@duoliang-onespan
            String customUrl = serviceConfig.customUrl().toLowerCase();
            String getUserURL = StringUtils.getAPIEndpoint(tenantName,environment, customUrl) + String.format(Constants.OSTID_API_VDP_GET_USER,usernameJsonValue.asString(),config.domain());
            HttpEntity getUserHttpEntity = RestUtils.doGet(getUserURL,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
            JSONObject getUserResponseJSON = getUserHttpEntity.getResponseJSON();
            if(!getUserHttpEntity.isSuccess()) {
                String error = getUserResponseJSON.getString("error");
                String message = getUserResponseJSON.getString("message");
                String requestJSON = "GET "+ getUserURL;

                String log_correction_id = getUserHttpEntity.getLog_correlation_id();

                if(Stream.of(message, error, log_correction_id).anyMatch(Objects::isNull)){
                    throw new NodeProcessException(JSON.toJSONString(getUserResponseJSON));
                }else {
                    JSONArray validationErrors = getUserResponseJSON.getJSONArray("validationErrors");
                    if(validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null){
                    	String errorMsgNoRetCodeWithValidation = StringUtils.getErrorMsgNoRetCodeWithValidation(message,log_correction_id,validationErrors.getJSONObject(0).getString("message"),requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithValidation);
                    }else{
                    	String errorMsgNoRetCodeWithoutValidation = StringUtils.getErrorMsgNoRetCodeWithoutValidation(message,log_correction_id,requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithoutValidation);
                    }
                }
            }
	        
            
            //API2: GET /v1/authenticators?serialNumber=VDP4938800&domain=duoliang-onespan&type=VIR10&assigned=true&offset=0&limit=20
            JSONArray authenticatorsJsonArray = getUserResponseJSON.getJSONArray("authenticators");
            
            if(authenticatorsJsonArray == null || authenticatorsJsonArray.size() < 1) {
	            throw new NodeProcessException("Can't find VIR10 authenticator for User " + usernameJsonValue.asString() +"!");
            }
            
            List<String> authenticatorsList = authenticatorsJsonArray.toJavaList(String.class);
            String vir10SerialNumber = null;
            String applicationName = null;
            for (String authenticator : authenticatorsList) {
                String getAuthenticatorURL = StringUtils.getAPIEndpoint(tenantName,environment, customUrl) + String.format(Constants.OSTID_API_VDP_GET_VIR10_AUTHENTICATOR,authenticator,config.domain());
                HttpEntity getAuthenticatorHttpEntity = RestUtils.doGet(getAuthenticatorURL,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
                JSONObject getAuthenticatorResponseJSON = getAuthenticatorHttpEntity.getResponseJSON();
                if(getAuthenticatorHttpEntity.isSuccess()) {
                	Integer total = getAuthenticatorResponseJSON.getInteger("total");
                	if(total != null && total > 0) {
                		JSONArray userAuthenticatorsJsonArray = getAuthenticatorResponseJSON.getJSONArray("authenticators");
                		for(int i = 0; i < userAuthenticatorsJsonArray.size(); i++) {
                			JSONObject userAuthenticatorJsonObject = userAuthenticatorsJsonArray.getJSONObject(i);
                			if(userAuthenticatorJsonObject.getString("serialNumber").equals(authenticator) &&
            				   userAuthenticatorJsonObject.getString("authenticatorType").equals("VIR10")
                			) {
                				vir10SerialNumber = authenticator;
                				JSONArray applicationsJsonArray = userAuthenticatorJsonObject.getJSONArray("applications");
                				if(applicationsJsonArray != null && applicationsJsonArray.size() > 0) {
                					for(int j = 0; j < applicationsJsonArray.size(); j++) {
                						JSONObject applicationJsonObject = applicationsJsonArray.getJSONObject(j);
                						if(applicationJsonObject.containsKey("type") && "RO".equals(applicationJsonObject.getString("type"))) {
            								applicationName = applicationJsonObject.containsKey("name") ? applicationJsonObject.getString("name") : "";
            								break;
                						}
                					}
                				}
                				break;
                			}
                		}
                	}
                }
			}
            
            if(StringUtils.isEmpty(vir10SerialNumber)) {
	            throw new NodeProcessException("Can't find VIR10 authenticator for User " + usernameJsonValue.asString() +"!");
            }
            
            //API3: POST /v1/authenticators/VDP4957024/applications/PASSWORD/generate-votp
            String generateVotpURL = StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + String.format(Constants.OSTID_API_VDP_GENERATE_VOTP,vir10SerialNumber,applicationName);

            String generateVotpJSON = String.format(Constants.OSTID_JSON_VDP_GENERATE_VOTP,
                    optionalAttributesStringBuilder.toString(),                              //param1
                    config.vdpDeliveryMethod().name()                                        //param2
            );
            logger.debug(loggerPrefix + "OS_Auth_VDPGenerateVOTPNode generateVotpJSON:" + generateVotpJSON);

            HttpEntity generateVotpHttpEntity = RestUtils.doPostJSON(generateVotpURL, generateVotpJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
            JSONObject generateVotpResponseJSON = generateVotpHttpEntity.getResponseJSON();

            if (!generateVotpHttpEntity.isSuccess()) {
                String log_correction_id = generateVotpHttpEntity.getLog_correlation_id();
                String message = generateVotpResponseJSON.getString("message");
                String requestJSON = "POST " + generateVotpURL + " : " + generateVotpJSON;

                if (Stream.of(log_correction_id, message).anyMatch(Objects::isNull)) {
                    throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(generateVotpResponseJSON));
                } else {
                    JSONArray validationErrors = generateVotpResponseJSON.getJSONArray("validationErrors");
                    if(validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null){
                    	String errorMsgNoRetCodeWithValidation = StringUtils.getErrorMsgNoRetCodeWithValidation(message,log_correction_id,validationErrors.getJSONObject(0).getString("message"),requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithValidation);
                    }else{
                    	String errorMsgNoRetCodeWithoutValidation = StringUtils.getErrorMsgNoRetCodeWithoutValidation(message,log_correction_id,requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithoutValidation);
                    }
                }
            }
            
	        return goTo(OS_Auth_VDPGenerateVOTPNode.GenerateVOTPOutcome.success).replaceSharedState(sharedState).build();
    	}catch (Exception ex) {
	   		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_VDPGenerateVOTP Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan Generate VOTP: " + ex.getMessage());
			return goTo(GenerateVOTPOutcome.error).build();	
	    }
    }

    private Action.ActionBuilder goTo(OS_Auth_VDPGenerateVOTPNode.GenerateVOTPOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum DeliveryMethod {
    	Email, SMS, Voice, Response
    }

    
    public enum GenerateVOTPOutcome {
        success,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSVdpGenerateVOTPOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_VDPGenerateVOTPNode.BUNDLE,
                    OS_Auth_VDPGenerateVOTPNode.OSVdpGenerateVOTPOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Auth_VDPGenerateVOTPNode.GenerateVOTPOutcome.success.name(), bundle.getString("successOutcome")),
                    new Outcome(OS_Auth_VDPGenerateVOTPNode.GenerateVOTPOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

