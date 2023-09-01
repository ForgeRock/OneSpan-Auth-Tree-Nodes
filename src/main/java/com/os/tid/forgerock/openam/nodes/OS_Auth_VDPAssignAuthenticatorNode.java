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
@Node.Metadata( outcomeProvider = OS_Auth_VDPAssignAuthenticatorNode.OSTIDVDPAssignAuthenticatorOutcomeProvider.class,
                configClass = OS_Auth_VDPAssignAuthenticatorNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_VDPAssignAuthenticatorNode implements Node {
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_VDPAssignAuthenticatorNode.class);
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_VDPAssignAuthenticatorNode";
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth VDP Assign Authenticator]" + OSAuthNodePlugin.logAppender;
    private final OS_Auth_VDPAssignAuthenticatorNode.Config config;

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
    }

    @Inject
    public OS_Auth_VDPAssignAuthenticatorNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_VDPAssignAuthenticatorNode started");
	        NodeState ns = context.getStateFor(this);
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        String usernameInSharedState = ns.get(Constants.OSTID_USERNAME_IN_SHARED_STATE) == null ? Constants.OSTID_DEFAULT_USERNAME : ns.get(Constants.OSTID_USERNAME_IN_SHARED_STATE).asString();
	        JsonValue usernameJsonValue = ns.get(usernameInSharedState);

	
	        if(CollectionsUtils.hasAnyNullValues(ImmutableList.of(usernameJsonValue))){
	            throw new NodeProcessException("OS_Auth_VDPAssignAuthenticatorNode has missing data!");
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
            if(authenticatorsJsonArray != null && authenticatorsJsonArray.size() > 0) {
	            List<String> authenticatorsList = authenticatorsJsonArray.toJavaList(String.class);
	            String vir10SerialNumber = null;
	            for (String authenticator : authenticatorsList) {
	                String getAuthenticatorURL = StringUtils.getAPIEndpoint(tenantName,environment, customUrl) + String.format(Constants.OSTID_API_VDP_GET_VIR10_AUTHENTICATOR,authenticator,config.domain());
	                HttpEntity getAuthenticatorHttpEntity = RestUtils.doGet(getAuthenticatorURL,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
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
	                return goTo(OS_Auth_VDPAssignAuthenticatorNode.VDPAssignAuthenticatorOutcome.success).build();
	            }
            }
	        
	        //API3: GET /v1/authenticators?type=VIR10&assigned=false&offset=0&limit=20
            String getVIR10AuthenticatorsURL = StringUtils.getAPIEndpoint(tenantName,environment, customUrl) + Constants.OSTID_API_VDP_GET_VIR10_AUTHENTICATORS;
            HttpEntity getVIR10AuthenticatorsHttpEntity = RestUtils.doGet(getVIR10AuthenticatorsURL,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
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
            		config.domain(),                               			  		//param1
            		usernameJsonValue.asString()                                //param2
            );

            String assignAuthenticatorURL = StringUtils.getAPIEndpoint(tenantName,environment, customUrl) + String.format(Constants.OSTID_API_VDP_ASSIGN_AUTHENTICATOR,serialNumber);
            HttpEntity assignAuthenticatorHttpEntity = RestUtils.doPostJSON(assignAuthenticatorURL, assignAuthenticatorJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
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
            
            return goTo(OS_Auth_VDPAssignAuthenticatorNode.VDPAssignAuthenticatorOutcome.success).build();
    	}catch (Exception ex) {
	   		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_VDPAssignAuthenticatorNode Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan VDP Assign Authenticator process: " + ex.getMessage());
			return goTo(VDPAssignAuthenticatorOutcome.error).build();	
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

