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
import com.os.tid.forgerock.openam.models.GenerateChallengeOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.nodes.OS_Auth_CheckActivationNode.ActivationStatusOutcome;
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
import java.util.*;
import java.util.stream.Stream;

/**
 * This node invokes the Generate Challenge API, which generates a random challenge.
 */
@Node.Metadata( outcomeProvider = OS_Auth_GenerateChallengeNode.OSTIDGenerateChallengeOutcomeProvider.class,
                configClass = OS_Auth_GenerateChallengeNode.Config.class,
                tags = {"OneSpan", "mfa", "basic authentication", "marketplace", "trustnetwork"})
public class OS_Auth_GenerateChallengeNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_GenerateChallengeNode";
    private final OSConfigurationsService serviceConfig;
    private final OS_Auth_GenerateChallengeNode.Config config;
    private static final String loggerPrefix = "[OneSpan Auth Generate Challenge][Marketplace] ";

    /**
     * Configuration for the OS Auth Generate Challenge Node.
     */
    public interface Config {
        /**
         * Length of the challenge excluding the optional check digit.
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default int length() {
            return 6;
        }

        /**
         * Signifies if a check digit must be appended to the challenge.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default boolean checkDigit() {
            return false;
        }
        /**
         * The key name in Shared State which represents the OCA username
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }
    }

    @Inject
    public OS_Auth_GenerateChallengeNode(@Assisted OS_Auth_GenerateChallengeNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_GenerateChallengeNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = serviceConfig.environment().name();
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	
	        String generateChallengeJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_GENERATE_CHALLENGE,
	                config.length(),                                    //param1
	                config.checkDigit()                                 //param2
	        );
            String url = StringUtils.getAPIEndpoint(tenantName, environment) + String.format(Constants.OSTID_API_ADAPTIVE_GENERATE_CHALLENGE, usernameJsonValue.asString(), tenantName);
            HttpEntity httpEntity = RestUtils.doPostJSON(url, generateChallengeJSON);
            JSONObject responseJSON = httpEntity.getResponseJSON();
            if (httpEntity.isSuccess()) {
                GenerateChallengeOutput generateChallengeOutput = JSON.toJavaObject(responseJSON, GenerateChallengeOutput.class);
                sharedState.put(Constants.OSTID_REQUEST_ID, generateChallengeOutput.getRequestID());
                sharedState.put(Constants.OSTID_CRONTO_MSG, StringUtils.stringToHex2(generateChallengeOutput.getChallenge()));

                return goTo(OS_Auth_GenerateChallengeNode.GenerateChallengeOutcome.success)
                        .replaceSharedState(sharedState)
                        .build();
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
			logger.error(loggerPrefix + "Exception occurred: " + ex.getMessage());
			logger.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
			ex.printStackTrace();
			context.getStateFor(this).putShared("OS_Auth_GenerateChallengeNode Exception", new Date() + ": " + ex.getMessage())
									 .putShared(Constants.OSTID_ERROR_MESSAGE, "OneSpan OCA Generate Challenge: " + ex.getMessage());
			return goTo(GenerateChallengeOutcome.error).build();
	    }
    }

    private Action.ActionBuilder goTo(OS_Auth_GenerateChallengeNode.GenerateChallengeOutcome outcome) {
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
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_GenerateChallengeNode.BUNDLE,
                    OS_Auth_GenerateChallengeNode.OSTIDGenerateChallengeOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Auth_GenerateChallengeNode.GenerateChallengeOutcome.success.name(), bundle.getString("successOutcome")),
                    new Outcome(OS_Auth_GenerateChallengeNode.GenerateChallengeOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

