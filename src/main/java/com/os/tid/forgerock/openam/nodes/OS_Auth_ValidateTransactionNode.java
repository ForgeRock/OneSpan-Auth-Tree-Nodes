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

import java.util.ArrayList;
import java.util.Collections;
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
import com.os.tid.forgerock.openam.models.GeneralResponseOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.SslUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

/**
 * This node invokes the Validate Transaction API, which validates and processes the authentication for a transaction request.
 */
@Node.Metadata( outcomeProvider = OS_Auth_ValidateTransactionNode.OSTID_Adaptive_SendTransactionNodeOutcomeProvider.class,
                configClass = OS_Auth_ValidateTransactionNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_ValidateTransactionNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_ValidateTransactionNode";
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_ValidateTransactionNode.class);
    private final OS_Auth_ValidateTransactionNode.Config config;
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth Validate Transaction]" + OSAuthNodePlugin.logAppender;

    /**
     * Configuration for the OneSpan Auth Validate Transaction Node.
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
         * Input payload object type.
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default ObjectType objectType() {
            return ObjectType.AdaptiveTransactionValidationInput;
        }

        /**
         * The key name in Shared State which represents the IAA/OCA username
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         * Which signature validation data to use
         */
        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default DataToSign dataToSign() { return DataToSign.transactionMessage; }

        /**
         * Configurable attributes in request JSON payload
         */
        @Attribute(order = 500)
        default List<String> standardDataToSign() {
            return ImmutableList.of("sourceAccount","destinationAccount","amountToTransfer");
        }

        /**
         * Signature for the transaction data.
         */
        @Attribute(order = 600, validators = RequiredValueValidator.class)
        default String signatureInSharedData() {
            return "signature";
        }

        /**
         * Object used to transfer FIDO AuthenticationResponse.
         */
        @Attribute(order = 700)
        default Map<String, String> fidoDataToSign() {
            return ImmutableMap.<String, String>builder()
                    .put("fidoProtocol", "fidoProtocol")
                    .put("authenticationResponse", "authenticationResponse")
                    .build();
        }

        /**
         * Array of key/value pairs representing the data fields of the transaction context.
         */
        @Attribute(order = 800)
        default Map<String, String> adaptiveDataToSign() {
            return Collections.emptyMap();
        }

        /**
         * Orchestration transaction data signing input. Delivery method for this transaction message is specified in the orchestrationDelivery field.
         */
        @Attribute(order = 900)
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

        /**
         * Configurable attributes in request JSON payload
         */
        @Attribute(order = 1000)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

        /**
         * Indicates whether a push notification should be sent, and/or if the orchestration command should be included in the response requestMessage.
         */
        @Attribute(order = 1100)
        default OrchestrationDelivery orchestrationDelivery() {
            return OrchestrationDelivery.both;
        }

        /**
         * Timeout in seconds.
         */
        @Attribute(order = 1200)
        default int timeout() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }

        /**
         * How to build and store visual code message in SharedState
         */
        @Attribute(order = 1300)
        default VisualCodeMessageOptions visualCodeMessageOptions() {
            return VisualCodeMessageOptions.sessionID;
        }
    }

    @Inject
    public OS_Auth_ValidateTransactionNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_ValidateTransactionNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
	
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE, config.userNameInSharedData());
	
	        boolean missOptionalAttr = false;
	        StringBuilder optionalAttributesStringBuilder = new StringBuilder(1000);
	        Map<String, String> optionalAttributesMap = config.optionalAttributes();
	        for (Map.Entry<String, String> entrySet : optionalAttributesMap.entrySet()) {
	            JsonValue jsonValue = sharedState.get(entrySet.getValue());
	            if (jsonValue.isString()) {
	                optionalAttributesStringBuilder.append("\"").append(entrySet.getKey()).append("\":\"").append(jsonValue.asString()).append("\",");
	            } else {
	                missOptionalAttr = true;
	            }
	        }
	
	        String sessionID = sharedState.get(Constants.OSTID_SESSIONID).isString() ? sharedState.get(Constants.OSTID_SESSIONID).asString() : StringUtils.stringToHex(UUID.randomUUID().toString());
	        String dataJSON = "";
	        String IAAJson = "";
	        boolean hasNullValue = false;
	        switch (config.dataToSign()){
	            case fido:
	                Map<String, String> fidoDataToSign = config.fidoDataToSign();
	                String fidoProtocol = fidoDataToSign.getOrDefault("fidoProtocol", "");
	                String authenticationResponse = fidoDataToSign.getOrDefault("authenticationResponse", "");
	                boolean isFido2 = sharedState.get(fidoProtocol).asString().equalsIgnoreCase("FIDO2");
	                hasNullValue = CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                        sharedState.get(fidoProtocol),
	                        sharedState.get(authenticationResponse)
	                )) ||  (isFido2 && sharedState.get(Constants.OSTID_REQUEST_ID).isNull());
	                if(!hasNullValue){
	                    dataJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_DATATOSIGN_FIDO,
	                            sharedState.get(fidoProtocol).asString(),                                                                                   //param1
	                            sharedState.get(authenticationResponse).asString(),                                                                         //param2
	                            isFido2? String.format(Constants.OSTID_JSON_ADAPTIVE_REQUESTID,sharedState.get(Constants.OSTID_REQUEST_ID).asString()):""   //param3
	                    );
	                }
	                break;
	            case standard:
	                List<String> dataValues = new ArrayList<>();
	                List<String> standardDataToSign = config.standardDataToSign();
	                for (String dataToSign : standardDataToSign) {
	                    if(!sharedState.get(dataToSign).isString()){
	                        hasNullValue = true;
	                    }else{
	                        dataValues.add("\""+sharedState.get(dataToSign).asString()+"\"");
	                    }
	                }
	                hasNullValue |= !sharedState.get(config.signatureInSharedData()).isString();
	                if(!hasNullValue){
	                    dataJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_DATATOSIGN_STANDARD,
	                            String.join(",", dataValues),                           //param1
	                            sharedState.get(config.signatureInSharedData()).asString()          //param2
	                    );
	                }
	                break;
	            case secureChannel:
	                hasNullValue = CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                        sharedState.get(Constants.OSTID_REQUEST_ID),
	                        sharedState.get(config.signatureInSharedData())
	                ));
	                if(!hasNullValue){
	                    dataJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_DATATOSIGN_SECURECHANNEL,
	                            sharedState.get(Constants.OSTID_REQUEST_ID).asString(),             //param1
	                            sharedState.get(config.signatureInSharedData()).asString()          //param2
	                    );
	                }
	                break;
	            case transactionMessage:
	                List<String> adaptiveAttributesList = new ArrayList<>();
	                List<String> dataFieldsList = new ArrayList<>();
	                Map<String, String> adaptiveAttributes = config.adaptiveAttributes();
	                for (Map.Entry<String, String> entry : adaptiveAttributes.entrySet()) {
	                    String nameInSharedState = entry.getValue();
	                    if(!sharedState.get(nameInSharedState).isString()){
	                        hasNullValue = true;
	                    }else{
	                        adaptiveAttributesList.add("\""+entry.getKey()+"\":\""+sharedState.get(nameInSharedState).asString()+"\"");
	                    }
	                }
	
	                //override transaction date to make sure the transaction date doesn't get tempered
	                Map<String, String> dataToSignMap = config.adaptiveDataToSign();
	                dataToSignMap.put("login",config.userNameInSharedData());
	                dataToSignMap.put("beneficiary",config.adaptiveAttributes().get("creditorName"));
	                dataToSignMap.put("iban",config.adaptiveAttributes().get("creditorIBAN"));
	                dataToSignMap.put("amount",config.adaptiveAttributes().get("amount"));
	                dataToSignMap.put("currency",config.adaptiveAttributes().get("currency"));
	
	                for (Map.Entry<String, String> entry : dataToSignMap.entrySet()) {
	                    String key = entry.getKey();
	                    String value = entry.getValue();
	                    logger.debug(loggerPrefix + "OSS data key= " + key);
	                    logger.debug(loggerPrefix + "OSS data value name= " + value);
	                    if(!sharedState.get(value).isString()){
	                        hasNullValue = true;
	                    }else{
	                        logger.debug(loggerPrefix + "OSS data value value= " + sharedState.get(value).asString());
	                        dataFieldsList.add(String.format(Constants.OSTID_JSON_ADAPTIVE_DATATOSIGN_TRANSACTIONMESSAGE_DATAFIELDS,
	                                key,
	                                sharedState.get(value).asString()));
	                    }
	                }
	
	                if(config.objectType() == ObjectType.AdaptiveTransactionValidationInput) {
	                    hasNullValue |= CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                            sharedState.get(Constants.OSTID_CDDC_JSON),
	                            sharedState.get(Constants.OSTID_CDDC_HASH),
	                            sharedState.get(Constants.OSTID_CDDC_IP)
	                    ));
	                }
	                if(!hasNullValue){
	                    String applicationRef = serviceConfig.applicationRef() != null ? serviceConfig.applicationRef() : "";
	                    String relationshipRefNameInSharedState = config.adaptiveAttributes().containsKey("relationshipRef") ? config.adaptiveAttributes().get("relationshipRef") : "relationshipRef";
	                    String relationshipRef = sharedState.get(relationshipRefNameInSharedState).isString() ? sharedState.get(relationshipRefNameInSharedState).asString():usernameJsonValue.asString();
	                    IAAJson = String.join(",",adaptiveAttributesList)+","+String.format(Constants.OSTID_JSON_ADAPTIVE_USER_LOGIN_IAA,
	                            sharedState.get(Constants.OSTID_CDDC_IP).asString(),
	                            sharedState.get(Constants.OSTID_CDDC_HASH).asString(),
	                            sharedState.get(Constants.OSTID_CDDC_JSON).asString(),
	                            relationshipRef,
	                            sessionID,
	                            applicationRef
	                            );
	                    dataJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_DATATOSIGN_TRANSACTIONMESSAGE, String.join(",",dataFieldsList));
	                }
	                break;
	        }
	
	        if (usernameJsonValue.isNull() || missOptionalAttr || hasNullValue){  //missing data
	            throw new NodeProcessException("Oopts, there are missing data for OneSpan Auth Validate Transaction Process!");
	        } 
	        
            String APIUrl = String.format(Constants.OSTID_API_ADAPTIVE_SEND_TRANSACTION, usernameJsonValue.asString(), config.domain());
            /**
             * 1.objectType
             * 2.dataToSign
             * 3.orchestrationDelivery
             * 4.timeout
             * 5.IAA
             **/
            //param 1
            String objectType = config.objectType().name();
            //param3
            String orchestrationDelivery = "";
            switch (config.orchestrationDelivery()) {
                case pushNotification:
                    orchestrationDelivery = String.format(Constants.OSTID_JSON_ADAPTIVE_ORCHESTRATIONDELIVERY, "\"pushNotification\"");
                    break;
                case requestMessage:
                    orchestrationDelivery = String.format(Constants.OSTID_JSON_ADAPTIVE_ORCHESTRATIONDELIVERY, "\"requestMessage\"");
                    break;
                case both:
                    orchestrationDelivery = String.format(Constants.OSTID_JSON_ADAPTIVE_ORCHESTRATIONDELIVERY, "\"pushNotification\",\"requestMessage\"");
                    break;
                case none:
                    break;
            }
            //param4: for now, API timeout will always set to 0, timeout specified in config will be used for visual code time out
            String timeout = String.format(Constants.OSTID_JSON_ADAPTIVE_TIMEOUT, 0);

            String sendTransactionJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_SEND_TRANSACTION,
                    objectType,                                                     //param1
                    dataJSON,                                                       //param2
                    orchestrationDelivery,                                          //param3
                    timeout,                                                        //param4
                    IAAJson                                                         //param5
            );
            logger.debug(loggerPrefix + "OS_Auth_ValidateTransactionNode JSON:" + sendTransactionJSON);

            String customUrl = serviceConfig.customUrl().toLowerCase();
            HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment, customUrl) + APIUrl, sendTransactionJSON,SslUtils.getSSLConnectionSocketFactory(serviceConfig));
            JSONObject responseJSON = httpEntity.getResponseJSON();

            if (httpEntity.isSuccess()) {
                GeneralResponseOutput loginOutput = JSON.toJavaObject(responseJSON, GeneralResponseOutput.class);
                int irmResponse = loginOutput.getRiskResponseCode();
                sharedState.put(Constants.OSTID_IRM_RESPONSE,irmResponse);
                sharedState.put(Constants.OSTID_SESSIONID,sessionID);
                sharedState.put(Constants.OSTID_REQUEST_ID, loginOutput.getRequestID());
                sharedState.put(Constants.OSTID_COMMAND,loginOutput.getRequestMessage());
                sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.timeout()));

                SendTransactionOutcome sendTransactionOutcome = SendTransactionOutcome.Error;

                switch (SessionStatus.valueOf(loginOutput.sessionStatus)){
                case accepted:
                    sendTransactionOutcome = SendTransactionOutcome.Accept;
                    break;
                case failed:
                    sendTransactionOutcome = SendTransactionOutcome.Error;
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Validate Transaction : User failed to authenticate!");
                    break;
                case refused:
                    sendTransactionOutcome = SendTransactionOutcome.Decline;
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Validate Transaction : User declined to authenticate!");
                    break;
                case timeout:
                	sendTransactionOutcome = SendTransactionOutcome.Error;
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Validate Transaction : Request times out!");
                    break;
                case unknown:
                	sendTransactionOutcome = SendTransactionOutcome.Error;
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Validate Event: Request status unknown!");
                    break;
                case pending:
                    if(irmResponse > -1) {
                        sharedState.put(Constants.OSTID_IRM_RESPONSE, irmResponse);

                        if (irmResponse == 0) {
                            sendTransactionOutcome = SendTransactionOutcome.Accept;
                        } else if (irmResponse == 1) {
                            sendTransactionOutcome = SendTransactionOutcome.Decline;
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Validate Transaction: Request been declined!");
                        } else if (Constants.OSTID_API_CHALLANGE_MAP.containsKey(irmResponse)) {
                            sendTransactionOutcome = SendTransactionOutcome.StepUp;
                        }

                        switch (config.visualCodeMessageOptions()) {
                            case sessionID:
                                sharedState.put(Constants.OSTID_CRONTO_MSG, StringUtils.stringToHex(sessionID));
                                break;
                            case requestID:
                                String crontoMsg = StringUtils.stringToHex(loginOutput.getRequestID() == null ? "" : loginOutput.getRequestID());
                                sharedState.put(Constants.OSTID_CRONTO_MSG, crontoMsg);
                                break;
                        }
                    }
                	break;
                }

                logger.debug(loggerPrefix + "OS_Auth_ValidateTransactionNode validate transaction outcome:" + sendTransactionOutcome.name());
                return goTo(sendTransactionOutcome).replaceSharedState(sharedState).build();
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
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_ValidateTransactionNode Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Validate Transactin Process: " + ex.getMessage());
			return goTo(SendTransactionOutcome.Error).build();	
	    }
        
    }

    public enum ObjectType {
        AdaptiveTransactionValidationInput, TransactionValidationInput
    }

    public enum DataToSign {
        fido, standard, secureChannel, transactionMessage
    }

    public enum SendTransactionOutcome {
        Accept, Decline, StepUp, Error
    }

    public enum OrchestrationDelivery {
        pushNotification, requestMessage, both, none
    }

    public enum VisualCodeMessageOptions {
        sessionID, requestID, none
    }
    public enum SessionStatus {
        unknown, pending, accepted, refused, timeout, failed
    }

    private Action.ActionBuilder goTo(SendTransactionOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTID_Adaptive_SendTransactionNodeOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_ValidateTransactionNode.BUNDLE,
                    OS_Auth_ValidateTransactionNode.OSTID_Adaptive_SendTransactionNodeOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(SendTransactionOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(SendTransactionOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(SendTransactionOutcome.StepUp.name(), bundle.getString("stepupOutcome")),
                    new Outcome(SendTransactionOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
