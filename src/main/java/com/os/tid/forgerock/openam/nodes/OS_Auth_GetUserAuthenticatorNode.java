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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
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
 * This node invokes the Add Device API, which continues and proceeds the OCA provisioning process.
 *
 */
@Node.Metadata( outcomeProvider = OS_Auth_GetUserAuthenticatorNode.OSTIDGetUserAuthenticatorOutcomeProvider.class,
                configClass = OS_Auth_GetUserAuthenticatorNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_GetUserAuthenticatorNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_GetUserAuthenticatorNode";
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth Get User Authenticator][Marketplace] ";
    private final OS_Auth_GetUserAuthenticatorNode.Config config;

    /**
     * Configuration for the OS Auth Add Device Node.
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
    }

    @Inject
    public OS_Auth_GetUserAuthenticatorNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_GetUserAuthenticatorNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE, config.userNameInSharedData());
	        
	
	        if(CollectionsUtils.hasAnyNullValues(ImmutableList.of(usernameJsonValue))){
	            throw new NodeProcessException("OS_Auth_GetUserAuthenticatorNode has missing data!");
	        }
	        
    		
	        //API1: GET /v1/users/duotest2305011@duoliang-onespan
            String getUserURL = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_VDP_GET_USER,usernameJsonValue.asString(),config.domain());
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
            if(authenticatorsJsonArray != null && authenticatorsJsonArray.size() > 0) {
	            List<String> authenticatorsList = authenticatorsJsonArray.toJavaList(String.class);
	            
	            Boolean hasVIR10Authenticator = false;
	            Boolean hasTYPAuthenticator = false;
	            
	            for (String authenticator : authenticatorsList) {
	                String getAuthenticatorURL = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_GET_USER_AUTHENTICATOR,authenticator,config.domain());
	                HttpEntity getAuthenticatorHttpEntity = RestUtils.doGet(getAuthenticatorURL,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
	                JSONObject getAuthenticatorResponseJSON = getAuthenticatorHttpEntity.getResponseJSON();
	                if(getAuthenticatorHttpEntity.isSuccess()) {
	                	Integer total = getAuthenticatorResponseJSON.getInteger("total");
	                	if(total !=null && total > 0) {
	                		JSONArray userAuthenticatorsJsonArray = getAuthenticatorResponseJSON.getJSONArray("authenticators");
	                		for(int i = 0; i < userAuthenticatorsJsonArray.size(); i++) {
	                			JSONObject userAuthenticatorJsonObject = userAuthenticatorsJsonArray.getJSONObject(i);
	                			if(userAuthenticatorJsonObject.getString("serialNumber").equals(authenticator)) {
	                				if(userAuthenticatorJsonObject.getString("authenticatorType").equals("VIR10")) {
	                					hasVIR10Authenticator = true;
		                			} 
	                				if(userAuthenticatorJsonObject.getString("authenticatorType").startsWith("TYP")) {
	                					hasTYPAuthenticator = true;
		                			} 
	                			} 
	                			}
	                		}
	                	}
	                }
	            
	            	
		            if(hasVIR10Authenticator && hasTYPAuthenticator) {
		                return goTo(OS_Auth_GetUserAuthenticatorNode.GetUserAuthenticatorOutcome.Both)
		                        .replaceSharedState(sharedState)
		                        .build();
		            }else if(hasVIR10Authenticator && !hasTYPAuthenticator) {
		            	 return goTo(OS_Auth_GetUserAuthenticatorNode.GetUserAuthenticatorOutcome.VIR10)
			                        .replaceSharedState(sharedState)
			                        .build();
		            }else if(!hasVIR10Authenticator && hasTYPAuthenticator) {
		            	 return goTo(OS_Auth_GetUserAuthenticatorNode.GetUserAuthenticatorOutcome.TYP)
			                        .replaceSharedState(sharedState)
			                        .build();
		            }else {
		            	 return goTo(OS_Auth_GetUserAuthenticatorNode.GetUserAuthenticatorOutcome.None)
		                         .replaceSharedState(sharedState)
		                         .build();
		            }
				}
            
            return goTo(OS_Auth_GetUserAuthenticatorNode.GetUserAuthenticatorOutcome.None)
                    .replaceSharedState(sharedState)
                    .build();
    	}catch (Exception ex) {
	   		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			JsonValue sharedState = context.sharedState;
		    JsonValue transientState = context.transientState;
			sharedState.put("OS_Auth_GetUserAuthenticatorNode Exception", new Date() + ": " + ex.getMessage());
			sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Get User Authenticator process: " + ex.getMessage());
			return goTo(GetUserAuthenticatorOutcome.None)
                     .replaceSharedState(sharedState)
                     .replaceTransientState(transientState)
                     .build();	
	    }
    }

    private Action.ActionBuilder goTo(GetUserAuthenticatorOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum GetUserAuthenticatorOutcome{
        TYP,
        VIR10,
        Both,
        None
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDGetUserAuthenticatorOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_GetUserAuthenticatorNode.BUNDLE,
            		OSTIDGetUserAuthenticatorOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(GetUserAuthenticatorOutcome.TYP.name(), bundle.getString("TYPOutcome")),
                    new Outcome(GetUserAuthenticatorOutcome.VIR10.name(), bundle.getString("VIR10Outcome")),
                    new Outcome(GetUserAuthenticatorOutcome.Both.name(), bundle.getString("BothOutcome")),
                    new Outcome(GetUserAuthenticatorOutcome.None.name(), bundle.getString("NoneOutcome"))
            		);
        }
    }
}

