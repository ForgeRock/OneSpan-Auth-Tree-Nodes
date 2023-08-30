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

import javax.security.auth.callback.Callback;

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
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.SMSException;

/**
 * This node invokes the Activate Device API, which finalizes the OCA provisioning process.
 *
 */
@Node.Metadata( outcomeProvider = OS_Auth_ActivateDeviceNode.OSTIDActivateDeviceOutcomeProvider.class,
                configClass = OS_Auth_ActivateDeviceNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_ActivateDeviceNode implements Node {
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_ActivateDeviceNode.class);
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_ActivateDeviceNode";
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth Activate Device]" + OSAuthNodePlugin.logAppender;

    /**
     * Configuration for the OS Auth Add Device Node.
     */
    public interface Config {
    }

    @Inject
    public OS_Auth_ActivateDeviceNode(@Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
    	try {
	        logger.debug(loggerPrefix + "OS_Auth_ActivateDeviceNode started");
	        NodeState ns = context.getStateFor(this);
	       
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        JsonValue registration_id = ns.get(Constants.OSTID_REGISTRATION_ID);
	        JsonValue signature = ns.get(Constants.OSTID_SIGNATURE);
	
	
	        if(CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                registration_id,
	                signature
	        ))){
	            throw new NodeProcessException("OS_Auth_ActivateDeviceNode has missing data!");
	        }else{
	            String activateDeviceJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_ACTIVATE_DEVICE,
	                    signature.asString()                                //param1
	            );
	
                String url = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_ADAPTIVE_ACTIVATE_DEVICE,registration_id.asString());
                HttpEntity httpEntity = RestUtils.doPostJSON(url, activateDeviceJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
                JSONObject responseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()) {
                    return goTo(OSTIDActivateDeviceOutcome.success).build();
                }else{
                    String error = responseJSON.getString("error");
                    String message = responseJSON.getString("message");
                    String requestJSON = "POST "+ url + " : " + activateDeviceJSON;

                    String log_correction_id = httpEntity.getLog_correlation_id();

                    if(Stream.of(message, error, log_correction_id).anyMatch(Objects::isNull)){
                        throw new NodeProcessException(JSON.toJSONString(responseJSON));
                    }else {
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
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_ActivateDeviceNode Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan OCA Activate Device process: " + ex.getMessage());
			return Action.goTo(OSTIDActivateDeviceOutcome.error.name()).build();
	    }

        
    }

    private Callback getStopCrontoCallback() {
        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(
                        "if (typeof $.CDDC_display == 'function') { " +
                        "    $.CDDC_display(false)();" +
                        "}" +
                        "if(typeof loginHelpers !== 'undefined'){" +
                        "   document.getElementsByClassName('btn-primary')[0].style.display = 'none';"+
                        "   document.getElementsByClassName('btn-primary')[0].click();"+
                        "}else{" +
                        "   document.getElementById('loginButton_0').style.display = 'none';"+
                        "   document.getElementById('loginButton_0').click();"+
                        "}"
        );
        return displayScriptCallback;
    }

    private Action.ActionBuilder goTo(OSTIDActivateDeviceOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum OSTIDActivateDeviceOutcome{
        success,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDActivateDeviceOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_ActivateDeviceNode.BUNDLE,
                    OSTIDActivateDeviceOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OSTIDActivateDeviceOutcome.success.name(), bundle.getString("successOutcome")),
                    new Outcome(OSTIDActivateDeviceOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

