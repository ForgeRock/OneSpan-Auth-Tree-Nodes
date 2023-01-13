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
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.GeneralResponseOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.nodes.OS_Auth_UserLoginNode.UserLoginOutcome;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.DateUtils;
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

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

/**
 * This node invokes the Validate Event API, which validates and processes the authentication for a non-monetary request.
 */
@Node.Metadata( outcomeProvider = OS_Auth_ValidateEventNode.OS_Auth_EventValidationNodeOutcomeProvider.class,
                configClass = OS_Auth_ValidateEventNode.Config.class,
                tags = {"OneSpan", "basic authentication", "mfa", "risk", "marketplace", "trustnetwork"})
public class OS_Auth_ValidateEventNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_ValidateEventNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OS_Auth_ValidateEventNode.Config config;
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Auth Validate Event][Marketplace] ";

    /**
     * Configuration for the OneSpan Auth Validate Event Node.
     */
    public interface Config {
        /**
         * Name of the non-monetary event type. String values for numeric keys related to custom event types are additionally accepted.
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default OS_Auth_ValidateEventNode.EventType eventType() {
            return OS_Auth_ValidateEventNode.EventType.SpecifyBelow;
        }

        /**
         * The hardcoded value of the event type.
         */
        @Attribute(order = 200)
        default String specifyEventType() {
            return "";
        }

        /**
         * The key name of the event type in SharedState.
         */
        @Attribute(order = 300)
        default String eventTypeInSharedState() {
            return "";
        }

        /**
         * Credentials to authenticate the user.
         */
        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default CredentialsType credentialsType() {
            return CredentialsType.none;
        }

        /**
         * The key name in Shared State which represents the IAA username
         */
        @Attribute(order = 500, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         * The key name in Shared State which represents the IAA/OCA password. Static password for the user or keyword to trigger out-of-band authentication.
         */
        @Attribute(order = 600, validators = RequiredValueValidator.class)
        default String passwordInTransientState() {
            return Constants.OSTID_DEFAULT_PASSKEY;
        }

        /**
         * Configurable attributes in request JSON payload
         */
        @Attribute(order = 700)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

        /**
         * Indicates whether a push notification should be sent, and/or if the orchestration command should be included in the response requestMessage.
         */
        @Attribute(order = 800)
        default OrchestrationDelivery orchestrationDelivery() {
            return OrchestrationDelivery.both;
        }

        /**
         * Timeout in seconds.
         */
        @Attribute(order = 900)
        default int timeout() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }

        /**
         * How to build and store visual code message in SharedState
         */
        @Attribute(order = 1000)
        default VisualCodeMessageOptions visualCodeMessageOptions() {
            return VisualCodeMessageOptions.sessionID;
        }
    }

    @Inject
    public OS_Auth_ValidateEventNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
	        logger.debug(loggerPrefix + "OS_Auth_ValidateEventNode started");
	        JsonValue sharedState = context.sharedState;
	        JsonValue transientState = context.transientState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	        String environment = serviceConfig.environment().name();
	
	        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
	        JsonValue cddcJsonJsonValue = sharedState.get(Constants.OSTID_CDDC_JSON);
	        JsonValue cddcHashJsonValue = sharedState.get(Constants.OSTID_CDDC_HASH);
	        JsonValue cddcIpJsonValue = sharedState.get(Constants.OSTID_CDDC_IP);
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
	
	        if (usernameJsonValue.isNull() || missOptionalAttr ||  CollectionsUtils.hasAnyNullValues(ImmutableList.of(
	                cddcJsonJsonValue,
	                cddcHashJsonValue,
	                cddcIpJsonValue
	        ))
	        ) {  //missing data
	            throw new NodeProcessException("Oopts, there are missing data for OneSpan Auth Event Validation Process!");
	        } 
	        
            String APIUrl = String.format(Constants.OSTID_API_ADAPTIVE_EVENT_VALIDATION, usernameJsonValue.asString(), tenantName);
            /**
             * 1.eventType
             * 2.credentials
             * 3.requestID
             * 4.orchestrationDelivery
             * 5.timeout
             * 6.IAA
             * 6.1 clientIP
             * 6.2 fingerprintHash
             * 6.3 fingerprintRaw
             * 6.4 relationshipRef
             * 6.5 sessionID
             * 6.6 applicationRef
             */
            //param 1
            String eventType = null;
            switch(config.eventType()) {
                case SpecifyBelow:
                    eventType = config.specifyEventType();
                    break;
                case ReadFromSharedState:
                    eventType = sharedState.get(config.eventTypeInSharedState()).asString();
                    break;
                default:
                    break;
            }
            //param2&8
            String credentials = "";
            String fido = "";
            switch (config.credentialsType()) {
                case fidoAuthenticator:
                    credentials = String.format(Constants.OSTID_JSON_ADAPTIVE_CREDENTIALS_FIDOAUTHENTICATOR, sharedState.get("authenticationResponse").asString());
                    fido = String.format(Constants.OSTID_JSON_ADAPTIVE_CREDENTIALS_FIDOAUTHENTICATOR_2, sharedState.get("fidoProtocol").asString());
                    break;
                case authenticator:
                    credentials = String.format(Constants.OSTID_JSON_ADAPTIVE_CREDENTIALS_AUTHENTICATOR, sharedState.get("OTP").asString());
                    break;
                case passKey:
                    credentials = String.format(Constants.OSTID_JSON_ADAPTIVE_CREDENTIALS_PASSKEY, transientState.get(config.passwordInTransientState()).asString());
                    break;
            }
            //param3
            String requestID = sharedState.get(Constants.OSTID_REQUEST_ID).isString() ? String.format(Constants.OSTID_JSON_ADAPTIVE_REQUESTID, sharedState.get(Constants.OSTID_REQUEST_ID).asString()) : "";
            //param4
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
            //param5: for now, API timeout will always set to 0, timeout specified in config will be used for visual code time out
            String timeout = String.format(Constants.OSTID_JSON_ADAPTIVE_TIMEOUT, 0);
            //param6
            String sessionID = sharedState.get(Constants.OSTID_SESSIONID).isString() ? sharedState.get(Constants.OSTID_SESSIONID).asString() : StringUtils.stringToHex(UUID.randomUUID().toString());
            String IAA = String.format(Constants.OSTID_JSON_ADAPTIVE_USER_LOGIN_IAA,
                    cddcIpJsonValue.asString(),                         //param6.1
                    cddcHashJsonValue.asString(),                       //param6.2
                    cddcJsonJsonValue.asString(),                       //param6.3
                    sharedState.get("relationshipRef").asString(),      //param6.4
                    sessionID,                                          //param6.5
                    serviceConfig.applicationRef()                      //param6.6
            );

            String eventValidationJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_EVENT_VALIDATION,
                    eventType,                                                      //param1
                    credentials,                                                    //param2
                    requestID,                                                      //param3
                    orchestrationDelivery,                                          //param4
                    timeout,                                                        //param5
                    IAA,                                                            //param6
                    optionalAttributesStringBuilder.toString(),                     //param7
                    fido                                                            //param8
            );
            logger.debug(loggerPrefix + "OS_Auth_ValidateEventNode request JSON:" + eventValidationJSON);

            HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl, eventValidationJSON);
            JSONObject responseJSON = httpEntity.getResponseJSON();

            if (httpEntity.isSuccess()) {
                GeneralResponseOutput responseOutput = JSON.toJavaObject(responseJSON, GeneralResponseOutput.class);
                int irmResponse = responseOutput.getRiskResponseCode();
                sharedState.put(Constants.OSTID_IRM_RESPONSE,irmResponse);
                sharedState.put(Constants.OSTID_SESSIONID,sessionID);
                sharedState.put(Constants.OSTID_REQUEST_ID, StringUtils.isEmpty(responseOutput.getRequestID())? requestID : responseOutput.getRequestID());
                sharedState.put(Constants.OSTID_COMMAND,responseOutput.getRequestMessage());
                sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.timeout()));

                EventValidationOutcome eventValidationOutcome = EventValidationOutcome.Error;

                if(irmResponse > -1) {
                    sharedState.put(Constants.OSTID_IRM_RESPONSE, irmResponse);
                    if(irmResponse == 0){
                        eventValidationOutcome = EventValidationOutcome.Accept;
                    }else if(irmResponse == 1){
                        eventValidationOutcome = EventValidationOutcome.Decline;
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Event Validation process: Request has been declined!");
                    }else if(Constants.OSTID_API_CHALLANGE_MAP.containsKey(irmResponse)){
                        eventValidationOutcome = EventValidationOutcome.StepUp;
                    }
                    switch (config.visualCodeMessageOptions()) {
                        case sessionID:
                            sharedState.put(Constants.OSTID_CRONTO_MSG, StringUtils.stringToHex(sessionID));
                            break;
                        case requestID:
                            String crontoMsg = StringUtils.stringToHex(responseOutput.getRequestID() == null ? "" : responseOutput.getRequestID());
                            sharedState.put(Constants.OSTID_CRONTO_MSG, crontoMsg);
                            break;
                    }
                }else{
                    switch (EventValidationSessionStatus.valueOf(responseOutput.getSessionStatus())){
                        case accepted:
                            eventValidationOutcome = EventValidationOutcome.Accept;
                            break;
                        case failed:
                            eventValidationOutcome = EventValidationOutcome.Error;
                            break;
                        case refused:
                            eventValidationOutcome = EventValidationOutcome.Decline;
                            break;
                    }
                }
                logger.debug(loggerPrefix + "OS_Auth_ValidateEventNode user login outcome:" + eventValidationOutcome.name());
                return goTo(eventValidationOutcome)
                        .replaceSharedState(sharedState)
                        .build();
            } else {
                String log_correction_id = httpEntity.getLog_correlation_id();
                String message = responseJSON.getString("message");
                String requestJSON = "POST " + StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl + " : " + eventValidationJSON;

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
			logger.error(loggerPrefix + "Exception occurred: " + ex.getMessage());
			logger.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
			ex.printStackTrace();
			context.getStateFor(this).putShared("OS_Auth_ValidateEventNode Exception", new Date() + ": " + ex.getMessage())
									 .putShared(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Event Validation process: " + ex.getMessage());
			return goTo(EventValidationOutcome.Error).build();
	    }
        
    }

    public enum EventType{
        SpecifyBelow, ReadFromSharedState
    }

    public enum CredentialsType {
        fidoAuthenticator, authenticator, passKey, none
    }

    public enum EventValidationOutcome {
        Accept, Decline, StepUp, Error
    }

    public enum EventValidationSessionStatus {
        unknown, pending, accepted, refused, timeout, failed
    }

    public enum OrchestrationDelivery {
        pushNotification, requestMessage, both, none
    }

    public enum VisualCodeMessageOptions {
        sessionID, requestID, none
    }

    private Action.ActionBuilder goTo(EventValidationOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OS_Auth_EventValidationNodeOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_ValidateEventNode.BUNDLE,
                    OS_Auth_ValidateEventNode.OS_Auth_EventValidationNodeOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(EventValidationOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(EventValidationOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(EventValidationOutcome.StepUp.name(), bundle.getString("stepupOutcome")),
                    new Outcome(EventValidationOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
