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
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.models.GeneralResponseOutput;
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

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

/**
 * This node invokes the User Register/Unregister Service API, in order to validate and process the registration/unregistration of a user.
 */
@Node.Metadata( outcomeProvider = OS_Auth_UserLoginNode.OSTID_Adaptive_UserLoginNode3OutcomeProvider.class,
                configClass = OS_Auth_UserLoginNode.Config.class,
                tags = {"OneSpan", "mfa"})
public class OS_Auth_UserLoginNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_UserLoginNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OS_Auth_UserLoginNode.Config config;
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OneSpan TID Adaptive User Login Node.
     */
    public interface Config {
        /**
         * @return
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default ObjectType objectType() {
            return ObjectType.AdaptiveLoginInput;
        }

        /**
         * @return
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default CredentialsType credentialsType() {
            return CredentialsType.none;
        }

        /**
         * @return
         */
        @Attribute(order = 300, validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         * @return
         */
        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default String passwordInTransientState() {
            return Constants.OSTID_DEFAULT_PASSKEY;
        }

        /**
         * Configurable attributes in request JSON payload
         *
         * @return
         */
        @Attribute(order = 500)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

        /**
         * @return
         */
        @Attribute(order = 600)
        default OrchestrationDelivery orchestrationDelivery() {
            return OrchestrationDelivery.pushNotification;
        }

        /**
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 700)
        default int timeout() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }


        /**
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 800)
        default VisualCodeMessageOptions visualCodeMessageOptions() {
            return VisualCodeMessageOptions.sessionID;
        }

    }

    @Inject
    public OS_Auth_UserLoginNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OS_Auth_UserLoginNode started");
        JsonValue sharedState = context.sharedState;
        JsonValue transientState = context.transientState;
        String tenantName = serviceConfig.tenantNameToLowerCase();
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

        if (usernameJsonValue.isNull() || missOptionalAttr || (config.objectType() == ObjectType.AdaptiveLoginInput && CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                cddcJsonJsonValue,
                cddcHashJsonValue,
                cddcIpJsonValue
        )))
        ) {  //missing data
            logger.debug("OS_Auth_UserLoginNode exception: Oopts, there's missing data for OneSpan TID Adaptive User Login Process!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE, "Oopts, there's missing data for OneSpan TID Adaptive User Login Process!");
            return goTo(UserLoginOutcome.Error)
                    .replaceSharedState(sharedState)
                    .build();
        } else {
            String APIUrl = String.format(Constants.OSTID_API_ADAPTIVE_USER_LOGIN, usernameJsonValue.asString(), tenantName);
            /**
             * 1.objectType
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
            String objectType = config.objectType().name();
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
            IAA = config.objectType() == ObjectType.AdaptiveLoginInput ? IAA : "";

            String userLoginJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_USER_LOGIN,
                    objectType,                                                     //param1
                    credentials,                                                    //param2
                    requestID,                                                      //param3
                    orchestrationDelivery,                                          //param4
                    timeout,                                                        //param5
                    IAA,                                                            //param6
                    optionalAttributesStringBuilder.toString(),                     //param7
                    fido                                                            //param8
            );
            logger.debug("OS_Auth_UserLoginNode user login JSON:" + userLoginJSON);

            try {
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl, userLoginJSON);
                JSONObject responseJSON = httpEntity.getResponseJSON();

                if (httpEntity.isSuccess()) {
                    GeneralResponseOutput loginOutput = JSON.toJavaObject(responseJSON, GeneralResponseOutput.class);
                    int irmResponse = loginOutput.getRiskResponseCode();
                    sharedState.put(Constants.OSTID_IRM_RESPONSE,irmResponse);
                    sharedState.put(Constants.OSTID_SESSIONID,sessionID);
                    sharedState.put(Constants.OSTID_REQUEST_ID, org.apache.commons.lang.StringUtils.isEmpty(loginOutput.getRequestID())? requestID : loginOutput.getRequestID());
                    sharedState.put(Constants.OSTID_COMMAND,loginOutput.getRequestMessage());

                    UserLoginOutcome userLoginOutcome = UserLoginOutcome.Error;

                    if(irmResponse > -1) {
                        sharedState.put(Constants.OSTID_IRM_RESPONSE, irmResponse);
                        if(irmResponse == 0){
                            userLoginOutcome = UserLoginOutcome.Accept;
                        }else if(irmResponse == 1){
                            userLoginOutcome = UserLoginOutcome.Decline;
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan TID Login process: Request been declined!");
                        }else if(Constants.OSTID_API_CHALLANGE_MAP.containsKey(irmResponse)){
                            userLoginOutcome = UserLoginOutcome.StepUp;
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
                    }else{
                        switch (UserLoginSessionStatus.valueOf(loginOutput.getSessionStatus())){
                            case accepted:
                                userLoginOutcome = UserLoginOutcome.Accept;
                                break;
                            case failed:
                                userLoginOutcome = UserLoginOutcome.Error;
                                break;
                            case refused:
                                userLoginOutcome = UserLoginOutcome.Decline;
                                break;
                        }
                    }

                    logger.debug("OS_Auth_UserLoginNode user login outcome:" + userLoginOutcome.name());
                    return goTo(userLoginOutcome)
                            .replaceSharedState(sharedState)
                            .build();
                } else {
                    String log_correction_id = httpEntity.getLog_correlation_id();
                    String message = responseJSON.getString("message") + StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl + " : " + userLoginJSON;

                    if (Stream.of(log_correction_id, message).anyMatch(Objects::isNull)) {
                        throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(responseJSON));
                    } else {
                        JSONArray validationErrors = responseJSON.getJSONArray("validationErrors");
                        if (validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null) {
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, StringUtils.getErrorMsgNoRetCodeWithValidation(message, log_correction_id, validationErrors.getJSONObject(0).getString("message")));         //error return from IAA server
                        } else {
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, StringUtils.getErrorMsgNoRetCodeWithoutValidation(message, log_correction_id));         //error return from IAA server
                        }

                        logger.debug("OS_Auth_UserLoginNode user login outcome:" + UserLoginOutcome.Error.name());

                        return goTo(UserLoginOutcome.Error)
                                .replaceSharedState(sharedState)
                                .build();
                    }
                }
            } catch (Exception e) {
                logger.debug("OS_Auth_UserLoginNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE, "Fail to Login!");                            //general error msg
                return goTo(UserLoginOutcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }

        }
    }

    public enum ObjectType {
        AdaptiveLoginInput, LoginInput
    }

    public enum CredentialsType {
        fidoAuthenticator, authenticator, passKey, none
    }

    public enum UserLoginOutcome {
        Accept, Decline, StepUp, Error
    }

    public enum UserLoginSessionStatus {
        unknown, pending, accepted, refused, timeout, failed
    }

    public enum OrchestrationDelivery {
        pushNotification, requestMessage, both, none
    }

    public enum VisualCodeMessageOptions {
        sessionID, requestID, none
    }

    private Action.ActionBuilder goTo(UserLoginOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTID_Adaptive_UserLoginNode3OutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_UserLoginNode.BUNDLE,
                    OS_Auth_UserLoginNode.OSTID_Adaptive_UserLoginNode3OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(UserLoginOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(UserLoginOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(UserLoginOutcome.StepUp.name(), bundle.getString("stepupOutcome")),
                    new Outcome(UserLoginOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
