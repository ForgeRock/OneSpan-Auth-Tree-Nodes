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
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.AddDeviceOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.nodes.OS_Auth_ActivateDeviceNode.OSTIDActivateDeviceOutcome;
import com.os.tid.forgerock.openam.nodes.OS_Auth_ValidateTransactionNode.SendTransactionOutcome;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * This node invokes the Add Device API, which continues and proceeds the OCA provisioning process.
 *
 */
@Node.Metadata( outcomeProvider = OS_Auth_VDPAssignAuthenticatorNode.OSTIDVDPAssignAuthenticatorOutcomeProvider.class,
                configClass = OS_Auth_VDPAssignAuthenticatorNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_VDPAssignAuthenticatorNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_VDPAssignAuthenticatorNode";
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth VDP Assign Authenticator][Marketplace] ";

    /**
     * Configuration for the OS Auth Add Device Node.
     */
    public interface Config {
    }

    @Inject
    public OS_Auth_VDPAssignAuthenticatorNode(@Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
    	try {
	        logger.debug(loggerPrefix + "OS_Auth_VDPAssignAuthenticatorNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        String usernameInSharedState = sharedState.get(Constants.OSTID_USERNAME_IN_SHARED_STATE) == null ? Constants.OSTID_DEFAULT_USERNAME : sharedState.get(Constants.OSTID_USERNAME_IN_SHARED_STATE).asString();
	        JsonValue usernameJsonValue = sharedState.get(usernameInSharedState);

	
	        if(CollectionsUtils.hasAnyNullValues(ImmutableList.of(usernameJsonValue))){
	            throw new NodeProcessException("OS_Auth_VDPAssignAuthenticatorNode has missing data!");
	        }
	        
    		
	        //API1: GET /v1/users/duotest2305011@duoliang-onespan
            String getUserURL = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_VDP_GET_USER,usernameJsonValue.asString(),tenantName);
            HttpEntity getUserHttpEntity = RestUtils.doGet(getUserURL);
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
            List<String> authenticatorsList = authenticatorsJsonArray.toJavaList(String.class);
            String vir10SerialNumber = null;
            for (String authenticator : authenticatorsList) {
                String getAuthenticatorURL = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_VDP_GET_AUTHENTICATOR,authenticator,tenantName);
                HttpEntity getAuthenticatorHttpEntity = RestUtils.doGet(getAuthenticatorURL);
                JSONObject getAuthenticatorResponseJSON = getAuthenticatorHttpEntity.getResponseJSON();
                if(getAuthenticatorHttpEntity.isSuccess()) {
                	Integer total = getAuthenticatorResponseJSON.getInteger("total");
                	if(total !=null && total > 0) {
                		JSONArray userAuthenticatorsJsonArray = getAuthenticatorResponseJSON.getJSONArray("authenticators");
                		for(int i = 0; i < userAuthenticatorsJsonArray.size(); i++) {
                			JSONObject userAuthenticatorJsonObject = userAuthenticatorsJsonArray.getJSONObject(i);
                			if(userAuthenticatorJsonObject.getString("serialNumber").equals(authenticator) &&
            				   userAuthenticatorJsonObject.getString("authenticatorType").equals("VIR10")
                			) {
                				vir10SerialNumber = authenticator;
                				break;
                			}
                		}
                	}
                }
			}
            
            if(!StringUtils.isEmpty(vir10SerialNumber)) {
                return goTo(OS_Auth_VDPAssignAuthenticatorNode.VDPAssignAuthenticatorOutcome.success)
                        .replaceSharedState(sharedState)
                        .build();
            }
            	        
	        
	        
	        //API3: GET /v1/authenticators?type=VIR10&assigned=false&offset=0&limit=20
            String getVIR10AuthenticatorsURL = StringUtils.getAPIEndpoint(tenantName,environment) + Constants.OSTID_API_VDP_GET_VIR10_AUTHENTICATORS;
            HttpEntity getVIR10AuthenticatorsHttpEntity = RestUtils.doGet(getVIR10AuthenticatorsURL);
            JSONObject getVIR10AuthenticatorsResponseJSON = getVIR10AuthenticatorsHttpEntity.getResponseJSON();
            if(!getVIR10AuthenticatorsHttpEntity.isSuccess()) {
                String error = getVIR10AuthenticatorsResponseJSON.getString("error");
                String message = getVIR10AuthenticatorsResponseJSON.getString("message");
                String requestJSON = "GET "+ getVIR10AuthenticatorsURL;

                String log_correction_id = getVIR10AuthenticatorsHttpEntity.getLog_correlation_id();

                if(Stream.of(message, error, log_correction_id).anyMatch(Objects::isNull)){
                    throw new NodeProcessException(JSON.toJSONString(getVIR10AuthenticatorsResponseJSON));
                }else {
                    JSONArray validationErrors = getVIR10AuthenticatorsResponseJSON.getJSONArray("validationErrors");
                    if(validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null){
                    	String errorMsgNoRetCodeWithValidation = StringUtils.getErrorMsgNoRetCodeWithValidation(message,log_correction_id,validationErrors.getJSONObject(0).getString("message"),requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithValidation);
                    }else{
                    	String errorMsgNoRetCodeWithoutValidation = StringUtils.getErrorMsgNoRetCodeWithoutValidation(message,log_correction_id,requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithoutValidation);
                    }
                }
            }
            
            
            //API4: POST /v1/authenticators/VDP4957024/assign
            Integer totalAuthticators = getVIR10AuthenticatorsResponseJSON.getInteger("total");
            if(totalAuthticators < 1) {
                throw new NodeProcessException("Failed to find unassigned VIR10 authenticator!");
            }
            
        	JSONObject vir10AuthenticatorJsonObject = getVIR10AuthenticatorsResponseJSON.getJSONArray("authenticators").getJSONObject(0);
        	String serialNumber = vir10AuthenticatorJsonObject.getString("serialNumber");
        	
            String assignAuthenticatorJSON = String.format(Constants.OSTID_JSON_VDP_ASSIGN_AUTHENTICATOR,
            		tenantName,                               			  		//param1
            		usernameJsonValue.asString()                                //param2
            );

            String assignAuthenticatorURL = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_VDP_ASSIGN_AUTHENTICATOR,serialNumber);
            HttpEntity assignAuthenticatorHttpEntity = RestUtils.doPostJSON(assignAuthenticatorURL, assignAuthenticatorJSON);
            JSONObject assignAuthenticatorResponseJSON = assignAuthenticatorHttpEntity.getResponseJSON();
            if(!assignAuthenticatorHttpEntity.isSuccess()) {
                String error = assignAuthenticatorResponseJSON.getString("error");
                String message = assignAuthenticatorResponseJSON.getString("message");
                String requestJSON = "POST " + assignAuthenticatorURL + " : " + assignAuthenticatorJSON;

                String log_correction_id = assignAuthenticatorHttpEntity.getLog_correlation_id();

                if(Stream.of(message, error, log_correction_id).anyMatch(Objects::isNull)){
                    throw new NodeProcessException(JSON.toJSONString(assignAuthenticatorResponseJSON));
                }else {
                    JSONArray validationErrors = assignAuthenticatorResponseJSON.getJSONArray("validationErrors");
                    if(validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null){
                    	String errorMsgNoRetCodeWithValidation = StringUtils.getErrorMsgNoRetCodeWithValidation(message,log_correction_id,validationErrors.getJSONObject(0).getString("message"),requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithValidation);
                    }else{
                    	String errorMsgNoRetCodeWithoutValidation = StringUtils.getErrorMsgNoRetCodeWithoutValidation(message,log_correction_id,requestJSON);
                        throw new NodeProcessException(errorMsgNoRetCodeWithoutValidation);
                    }
                }
            }
            
            return goTo(OS_Auth_VDPAssignAuthenticatorNode.VDPAssignAuthenticatorOutcome.success)
                    .replaceSharedState(sharedState)
                    .build();
    	}catch (Exception ex) {
	   		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			JsonValue sharedState = context.sharedState;
		    JsonValue transientState = context.transientState;
			sharedState.put("OS_Auth_VDPAssignAuthenticatorNode Exception", new Date() + ": " + ex.getMessage());
			sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan VDP Assign Authenticator process: " + ex.getMessage());
			return goTo(VDPAssignAuthenticatorOutcome.error)
                     .replaceSharedState(sharedState)
                     .replaceTransientState(transientState)
                     .build();	
	    }
    }

    private Action.ActionBuilder goTo(VDPAssignAuthenticatorOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum VDPAssignAuthenticatorOutcome{
        success,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDVDPAssignAuthenticatorOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_VDPAssignAuthenticatorNode.BUNDLE,
            		OSTIDVDPAssignAuthenticatorOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(VDPAssignAuthenticatorOutcome.success.name(), bundle.getString("successOutcome")),
                    new Outcome(VDPAssignAuthenticatorOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

