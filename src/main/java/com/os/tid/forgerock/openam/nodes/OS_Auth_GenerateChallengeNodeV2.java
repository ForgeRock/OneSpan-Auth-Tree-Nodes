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

import com.os.tid.forgerock.openam.utils.json.JSON;
import com.os.tid.forgerock.openam.utils.json.JSONArray;
import com.os.tid.forgerock.openam.utils.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.GenerateChallengeOutput;
import com.os.tid.forgerock.openam.models.v2.HttpEntity;
import com.os.tid.forgerock.openam.utils.v2.RestUtils;
import com.os.tid.forgerock.openam.utils.v2.SslUtils;
import com.os.tid.forgerock.openam.utils.v2.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

/**
 * This node invokes the Generate Challenge API, which generates a random challenge.
 */
@Node.Metadata( outcomeProvider = OS_Auth_GenerateChallengeNodeV2.OSTIDGenerateChallengeOutcomeProvider.class,
                configClass = OS_Auth_GenerateChallengeNodeV2.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
@Node.VersionMetadata(name = "OS_Auth_GenerateChallengeNode", version = 2)
public class OS_Auth_GenerateChallengeNodeV2 implements Node {
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_GenerateChallengeNodeV2.class);
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_GenerateChallengeNodeV2";
    private final OSConfigurationsService serviceConfig;
    private final OS_Auth_GenerateChallengeNodeV2.Config config;
    private static final String loggerPrefix = "[OneSpan Auth Generate Challenge]" + OSAuthNodePlugin.logAppender;

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
         * Length of the challenge excluding the optional check digit.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default int length() {
            return 6;
        }

        /**
         * Signifies if a check digit must be appended to the challenge.
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default boolean checkDigit() {
            return false;
        }
        /**
         * The key name in Shared State which represents the OCA username
         */
        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }
    }

    @Inject
    public OS_Auth_GenerateChallengeNodeV2(@Assisted OS_Auth_GenerateChallengeNodeV2.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_GenerateChallengeNodeV2 started");
            JsonValue sharedState = context.sharedState;
            
	        String tenantName = StringUtils.isEmpty(serviceConfig.tenantName())? "" : serviceConfig.tenantName().toLowerCase();
	        String customUrl = StringUtils.isEmpty(serviceConfig.customUrl())? "" : serviceConfig.customUrl().toLowerCase();         	        
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	        
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	
	        String generateChallengeJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_GENERATE_CHALLENGE,
	                StringUtils.escapeJson(String.valueOf(config.length())),          //param1
	                StringUtils.escapeJson(String.valueOf(config.checkDigit()))         //param2
	        );
            String url = StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + String.format(Constants.OSTID_API_ADAPTIVE_GENERATE_CHALLENGE, StringUtils.encodeUrlValue(usernameJsonValue.asString()), StringUtils.encodeUrlValue(config.domain()));
            HttpEntity httpEntity = RestUtils.doPostJSON(url, generateChallengeJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
            JSONObject responseJSON = httpEntity.getResponseJSON();
            if (httpEntity.isSuccess()) {
                GenerateChallengeOutput generateChallengeOutput = JSON.toJavaObject(responseJSON, GenerateChallengeOutput.class);
                sharedState.put(Constants.OSTID_REQUEST_ID, generateChallengeOutput.getRequestID());
                sharedState.put(Constants.OSTID_CRONTO_MSG, StringUtils.stringToHex2(generateChallengeOutput.getChallenge()));

                return goTo(OS_Auth_GenerateChallengeNodeV2.GenerateChallengeOutcome.success).replaceSharedState(sharedState).build();
            } else {
                String error = responseJSON.getString("error");
                String message = responseJSON.getString("message");
                String requestJSON = "POST "+ url + " : " + generateChallengeJSON;

                String log_correction_id = httpEntity.getLog_correlation_id();

                if (Stream.of(message, error, log_correction_id).anyMatch(Objects::isNull)) {
                    throw new NodeProcessException(JSON.toJSONString(responseJSON));
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
	   		String stackTrace = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
		    context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_GenerateChallengeNodeV2 Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan OCA Generate Challenge: " + ex.getMessage());
			return goTo(GenerateChallengeOutcome.error).build();	
	    }
    }

    private Action.ActionBuilder goTo(OS_Auth_GenerateChallengeNodeV2.GenerateChallengeOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum GenerateChallengeOutcome {
        success,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDGenerateChallengeOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_GenerateChallengeNodeV2.BUNDLE,
                    OS_Auth_GenerateChallengeNodeV2.OSTIDGenerateChallengeOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Auth_GenerateChallengeNodeV2.GenerateChallengeOutcome.success.name(), bundle.getString("successOutcome")),
                    new Outcome(OS_Auth_GenerateChallengeNodeV2.GenerateChallengeOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

