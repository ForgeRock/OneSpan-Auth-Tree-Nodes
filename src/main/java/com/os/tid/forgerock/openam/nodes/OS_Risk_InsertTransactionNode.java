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
import com.google.common.collect.ImmutableMap;
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
 * This node invokes the Risk Analytics Insert Transaction API, which validates and returns the result of a send transaction request.
 */
@Node.Metadata( outcomeProvider = OS_Risk_InsertTransactionNode.OSTID_Risk_InsertTransactionNodeOutcomeProvider.class,
                configClass = OS_Risk_InsertTransactionNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Risk_InsertTransactionNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Risk_InsertTransactionNode";
    private final Logger logger = LoggerFactory.getLogger(OS_Risk_InsertTransactionNode.class);
    private final OS_Risk_InsertTransactionNode.Config config;
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Risk Analytics Send Transaction]" + OSAuthNodePlugin.logAppender;

    /**
     * Configuration for the OS Risk Insert Transaction Node.
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
         * The key name in Shared State which represents the IAA/OCA username
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         * Orchestration transaction data signing input. Delivery method for this transaction message is specified in the orchestrationDelivery field.
         */
        @Attribute(order = 300)
        default Map<String, String> adaptiveAttributes() {
            return ImmutableMap.<String, String>builder()
                    .put("accountRef", "accountRef")
                    .put("amount", "amount")
                    .put("currency", "currency")
                    .put("transactionType", "transactionType")
                    .put("creditorBank", "creditorBank")
                    .put("creditorIBAN", "creditorIBAN")
                    .put("creditorName", "creditorName")
                    .put("debtorIBAN", "debtorIBAN")
                    .build();
        }
    }

    @Inject
    public OS_Risk_InsertTransactionNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Risk_InsertTransactionNode started");
            JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        boolean missAttr = false;
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        missAttr |= !usernameJsonValue.isString();
            sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE, config.userNameInSharedData());
	
	        StringBuilder attributesStringBuilder = new StringBuilder(1000);
	        Map<String, String> attributesMap = config.adaptiveAttributes();
	        for (Map.Entry<String, String> entrySet : attributesMap.entrySet()) {
	            JsonValue jsonValue = sharedState.get(entrySet.getValue());
	            if (jsonValue.isString()) {
	                attributesStringBuilder.append("\"").append(entrySet.getKey()).append("\":\"").append(jsonValue.asString()).append("\",");
	            } else {
	                missAttr = true;
	            }
	        }
	
	        String sessionID = sharedState.get(Constants.OSTID_SESSIONID).isString() ? sharedState.get(Constants.OSTID_SESSIONID).asString() : StringUtils.stringToHex(UUID.randomUUID().toString());
	
	        missAttr |= CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                sharedState.get(Constants.OSTID_CDDC_JSON),
	                sharedState.get(Constants.OSTID_CDDC_HASH),
	                sharedState.get(Constants.OSTID_CDDC_IP)
	        ));
	        String applicationRef = serviceConfig.applicationRef() != null ? serviceConfig.applicationRef() : "";
	
	        if(missAttr) { //missing data
	            logger.debug(loggerPrefix + JSON.toJSONString(sharedState));
	            throw new NodeProcessException("Oopts, there are missing data for OneSpan Risk Insert Transaction Node!");
	        }
	        
	        /**
	         * 1.attributes
	         * 2.CDDC IP
	         * 3.CDDC Hash
	         * 4.CDDC Json
	         * 5.session ID
	         * 6.application reference
	         * 7.relationship ID
	         **/
	        
            String relationshipRef = sharedState.get("relationshipRef").isString() ? sharedState.get("relationshipRef").asString():usernameJsonValue.asString();
	        
            String sendTransactionJSON = String.format(Constants.OSTID_JSON_RISK_SEND_TRANSACTION,
                    attributesStringBuilder.toString(),                              //param1
                    sharedState.get(Constants.OSTID_CDDC_IP).asString(),             //param2
                    sharedState.get(Constants.OSTID_CDDC_HASH).asString(),           //param3
                    sharedState.get(Constants.OSTID_CDDC_JSON).asString(),           //param4
                    sessionID,                                                       //param5
                    applicationRef,                                                  //param6
                    relationshipRef                                     			 //param7
            );
            String APIUrl = Constants.OSTID_API_RISK_SEND_TRANSACTION;
            String customUrl = serviceConfig.customUrl().toLowerCase();
            HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + APIUrl, sendTransactionJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
            JSONObject responseJSON = httpEntity.getResponseJSON();

            if (httpEntity.isSuccess()) {
                int riskResponseCode = responseJSON.getIntValue("riskResponseCode");
                sharedState.put(Constants.OSTID_RISK_RESPONSE_CODE, riskResponseCode);
                sharedState.put(Constants.OSTID_RISK_RESPONSE_CODE2, riskResponseCode);

                RiskTransactionOutcome riskTransactionOutcome = RiskTransactionOutcome.Error;
                if (riskResponseCode == 0) {
                    riskTransactionOutcome = RiskTransactionOutcome.Accept;
                } else if (riskResponseCode == 1) {
                    riskTransactionOutcome = RiskTransactionOutcome.Decline;
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Risk Insert Transaction: Request has been declined!");
                } else{
                    riskTransactionOutcome = RiskTransactionOutcome.Challenge;
                }
                return goTo(riskTransactionOutcome).build();
            } else {
                String log_correction_id = httpEntity.getLog_correlation_id();
                String message = responseJSON.getString("message");
                String requestJSON = "POST " + StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + APIUrl + " : " + sendTransactionJSON;

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
			context.getStateFor(this).putShared(loggerPrefix + "OS_Risk_InsertTransactionNode Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan Risk Insert Transaction: " + ex.getMessage());
			return goTo(RiskTransactionOutcome.Error).build();	
	    }
    }

    public enum RiskTransactionOutcome {
        Accept, Decline, Challenge, Error
    }

    private Action.ActionBuilder goTo(RiskTransactionOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTID_Risk_InsertTransactionNodeOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Risk_InsertTransactionNode.BUNDLE,
                    OSTID_Risk_InsertTransactionNodeOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(RiskTransactionOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(RiskTransactionOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(RiskTransactionOutcome.Challenge.name(), bundle.getString("challengeOutcome")),
                    new Outcome(RiskTransactionOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
