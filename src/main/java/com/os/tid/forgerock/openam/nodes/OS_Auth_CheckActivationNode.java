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
import java.util.ResourceBundle;

import org.forgerock.json.JsonValue;
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
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.SslUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.SMSException;

/**
 * This node invokes the Check Activation Status API, which checks the status of a pending activation of a device.
 *
 */
@Node.Metadata( outcomeProvider = OS_Auth_CheckActivationNode.OSTIDCheckActivateOutcomeProvider.class,
                configClass = OS_Auth_CheckActivationNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_CheckActivationNode implements Node {
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_CheckActivationNode.class);
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_CheckActivationNode";
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth Check Activation]" + OSAuthNodePlugin.logAppender;

    /**
     * Configuration for the OS Auth Check Activate Node.
     */
    public interface Config {
    }

    @Inject
    public OS_Auth_CheckActivationNode(@Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
    	try {
	        logger.debug(loggerPrefix + "OS_Auth_CheckActivationNode started");
	        JsonValue sharedState = context.sharedState;

	        String tenantName = StringUtils.isEmpty(serviceConfig.tenantName())? "" : serviceConfig.tenantName().toLowerCase();
	        String customUrl = StringUtils.isEmpty(serviceConfig.customUrl())? "" : serviceConfig.customUrl().toLowerCase();      
	        
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        //1. go to next
	        JsonValue ostid_cronto_status = sharedState.get(Constants.OSTID_CRONTO_STATUS);
	        if(ostid_cronto_status.isString()){
	            ActivationStatusOutcome ostid_cronto_status_enum = ActivationStatusOutcome.valueOf(ostid_cronto_status.asString());
	            sharedState.remove(Constants.OSTID_CRONTO_STATUS);
	            return goTo(ostid_cronto_status_enum).replaceSharedState(sharedState).build();
	        }
	
	        //2. call API and send script to remove visual code
	        JsonValue usernameJsonValue = sharedState.get(sharedState.get(Constants.OSTID_USERNAME_IN_SHARED_STATE).asString());
	        JsonValue eventExpiryJsonValue = sharedState.get(Constants.OSTID_EVENT_EXPIRY_DATE);
	        ActivationStatusOutcome activationStatusEnum;
	        if (!usernameJsonValue.isString() || usernameJsonValue.asString().isEmpty()) {
	            throw new NodeProcessException("Username is missing!");
	        } else if(DateUtils.hasExpired(eventExpiryJsonValue.asString())){
	            activationStatusEnum = ActivationStatusOutcome.timeout;
	            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan Auth Check Activation: Your session has timed out!");
	        } else {
	            String checkActivationJSON = String.format(Constants.OSTID_JSON_CHECK_ACTIVATION,
	                    usernameJsonValue.asString(),                            //param1
	                    Constants.OSTID_DEFAULT_CHECK_ACTIVATION_TIMEOUT         //param2
	            );
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName,environment, customUrl) + Constants.OSTID_API_CHECK_ACTIVATION, checkActivationJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
                JSONObject checkActivationResponseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()){
                    String activationStatus = checkActivationResponseJSON.getString(Constants.OSTID_RESPONSE_CHECK_ACTIVATION_STATUS);
                    activationStatusEnum = ActivationStatusOutcome.valueOf(activationStatus);
                }else{
                    String message = checkActivationResponseJSON.getString("message");
                    if(message == null){
                        throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(checkActivationResponseJSON));
                    }else {
                        throw new NodeProcessException(message);
                    }
                }
	        }
	
	        switch (activationStatusEnum) {
	            case pending:
	                return goTo(ActivationStatusOutcome.pending).replaceSharedState(sharedState).build();
	            case activated:
	                return goTo(ActivationStatusOutcome.activated).replaceSharedState(sharedState).build();
	            case timeout:
	                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan Auth Check Activation: Your session has timed out!");
	                return goTo(ActivationStatusOutcome.timeout).replaceSharedState(sharedState).build();
	            case unknown:
	                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan Auth Check Activation: Status Unknown!");
	                return goTo(ActivationStatusOutcome.unknown).replaceSharedState(sharedState).build();
	            default:
	                return goTo(ActivationStatusOutcome.pending).replaceSharedState(sharedState).build();
	        }
    	}catch (Exception ex) {
    		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_CheckActivationNode Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Check Activation: " + ex.getMessage());
			return goTo(ActivationStatusOutcome.error).build();	
	    }
    }

    private Action.ActionBuilder goTo(ActivationStatusOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum ActivationStatusOutcome{
        pending,
        activated,
        timeout,
        unknown,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDCheckActivateOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_CheckActivationNode.BUNDLE,
                    OSTIDCheckActivateOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(ActivationStatusOutcome.pending.name(), bundle.getString("pendingOutcome")),
                    new Outcome(ActivationStatusOutcome.activated.name(), bundle.getString("activatedOutcome")),
                    new Outcome(ActivationStatusOutcome.timeout.name(), bundle.getString("timeoutOutcome")),
                    new Outcome(ActivationStatusOutcome.unknown.name(), bundle.getString("unknownOutcome")),
                    new Outcome(ActivationStatusOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

