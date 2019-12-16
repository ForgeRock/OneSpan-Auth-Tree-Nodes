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
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.ExceptionOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.models.UserRegisterOutput;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 *
 *
 * <p></p>
 */
@Node.Metadata(outcomeProvider = OSTIDUserRegisterNode.OSTIDUserRegisterOutcomeProvider.class,
            configClass = OSTIDUserRegisterNode.Config.class)
public class OSTIDUserRegisterNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTIDUserRegisterNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDUserRegisterNode.Config config;
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OneSpan TID User Register Node.
     */
    public interface Config {
        /**
         *
         * @return
         */
        @Attribute(order = 100)
        default NodeFunction nodeFunction() {
            return NodeFunction.UserRegister;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 200,validators = RequiredValueValidator.class)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 300,validators = RequiredValueValidator.class)
        default String passwordInTransientState() {
            return Constants.OSTID_DEFAULT_PASSKEY;
        }

        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 400,validators = RequiredValueValidator.class)
        default int activationTokenExpiry() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }
    }

    @Inject
    public OSTIDUserRegisterNode(@Assisted Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context){
        logger.debug("OSTIDUserRegisterNode started");
        JsonValue sharedState = context.sharedState;
        JsonValue transientState = context.transientState;
        String tenantName = serviceConfig.tenantName();
        String environment = serviceConfig.environment();

        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
        JsonValue cddcJsonJsonValue = sharedState.get(Constants.OSTID_CDDC_JSON);
        JsonValue cddcHashJsonValue = sharedState.get(Constants.OSTID_CDDC_HASH);
        JsonValue cddcIpJsonValue = sharedState.get(Constants.OSTID_CDDC_IP);

        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE,config.userNameInSharedData());

        boolean isPasskeyIncluded = true;
        String passKey = "";
        if(config.nodeFunction() == NodeFunction.UserRegister){
            JsonValue passwordJsonValue = transientState.get(config.passwordInTransientState());
            if(!passwordJsonValue.isString()){
                isPasskeyIncluded = false;
            }else{
                passKey = String.format(Constants.OSTID_JSON_PASSKEY,passwordJsonValue.asString());
            }
        }

        //todo, return more granular error msg
        if (!isPasskeyIncluded ||
            CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                usernameJsonValue,
                cddcJsonJsonValue,
                cddcHashJsonValue,
                cddcIpJsonValue
        ))
        ) {  //collected data is not intact
            logger.debug("OSTIDUserRegisterNode exception: Oopts, there's missing data for OneSpan TID User Register Process!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan TID User Register Process!");
            return goTo(UserRegisterOutcome.Error)
                    .replaceSharedState(sharedState)
                    .build();
        } else {
            String sessionIdentifier = UUID.randomUUID().toString();
            try {
                if(sharedState.get(Constants.OSTID_SESSIONID).isString()) {
                    sessionIdentifier = Hex.decodeHex(sharedState.get(Constants.OSTID_SESSIONID).asString().toCharArray()).toString();
                }
            } catch (DecoderException e) {
                logger.debug("OSTIDUserRegisterNode exception: fail to parse session ID: " + sharedState.get(Constants.OSTID_SESSIONID).asString());
            }

            String userRegisterJSON = String.format(Constants.OSTID_JSON_USER_REGISTER,
                    usernameJsonValue.asString(),                                                    //param1
                    cddcIpJsonValue.asString(),                                                      //param2
                    cddcHashJsonValue.asString(),                                                    //param3
                    cddcJsonJsonValue.asString(),                                                    //param4
                    sessionIdentifier,                                                               //param5
                    passKey                                                                          //param6
            );

            String APIUrl = config.nodeFunction() == NodeFunction.UserRegister ? Constants.OSTID_API_USER_REGISTER : Constants.OSTID_API_USER_UNREGISTER;

            try {
                    HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment) + APIUrl, userRegisterJSON);
                    JSONObject userRegisterResponseJSON = httpEntity.getResponseJSON();
                    if (httpEntity.isSuccess()) {
                        UserRegisterOutput userRegisterResponse = JSON.toJavaObject(userRegisterResponseJSON, UserRegisterOutput.class);

                        if(config.nodeFunction() == NodeFunction.UserRegister) {
                            //"02;user11071;111;duoliang11071-mailin;U6gOkj13;duoliang11071-mailin"
                            String activationCode = userRegisterResponse.getActivationPassword();
                            String crontoValueRaw = String.format(Constants.OSTID_CRONTO_FORMULA,
                                    Constants.OSTID_API_VERSION,        //param1
                                    usernameJsonValue.asString(),       //param2
                                    tenantName,                         //param3
                                    activationCode,                     //param4
                                    tenantName                          //param5
                            );
                            String crontoValueHex = StringUtils.toHex(crontoValueRaw);

                            sharedState.put(Constants.OSTID_SESSIONID, StringUtils.toHex(sessionIdentifier));
                            sharedState.put(Constants.OSTID_CRONTO_MSG, crontoValueHex);
                            sharedState.put(Constants.OSTID_DIGI_SERIAL, userRegisterResponse.getDigipassSerial());
                            sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.activationTokenExpiry()));
//                    sharedState.remove(Constants.OSTID_CDDC_JSON);
//                    sharedState.remove(Constants.OSTID_CDDC_HASH);
//                    sharedState.remove(Constants.OSTID_CDDC_IP);

                        }else if(config.nodeFunction() == NodeFunction.UserUnregister){
                            sharedState.put(Constants.OSTID_SESSIONID, StringUtils.toHex(sessionIdentifier));
                        }
                        return goTo(UserRegisterOutcome.Success)
                                .replaceSharedState(sharedState)
                                .replaceTransientState(transientState)
                                .build();
                    } else {
                        String message = userRegisterResponseJSON.getString("message");
                        if (message == null) {
                            throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(userRegisterResponseJSON));
                        }
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE, message);
                        return goTo(UserRegisterOutcome.Error)
                                .replaceSharedState(sharedState)
                                .build();
                    }
            } catch (Exception e) {
                logger.debug("OSTIDUserRegisterNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Fail to Register User to OneSpan TID!");
                return goTo(UserRegisterOutcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }
        }
    }

    public enum NodeFunction{
        UserRegister,UserUnregister
    }
    public enum UserRegisterOutcome{
        Success,Error
    }
    private Action.ActionBuilder goTo(OSTIDUserRegisterNode.UserRegisterOutcome outcome) {
        return Action.goTo(outcome.name());
    }
    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDUserRegisterOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTIDUserRegisterNode.BUNDLE,
                    OSTIDUserRegisterNode.OSTIDUserRegisterOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OSTIDUserRegisterNode.UserRegisterOutcome.Success.name(), bundle.getString("successOutcome")),
                    new Outcome(OSTIDUserRegisterNode.UserRegisterOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
