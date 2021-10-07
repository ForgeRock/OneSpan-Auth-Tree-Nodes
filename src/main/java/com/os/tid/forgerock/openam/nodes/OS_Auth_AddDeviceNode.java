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
import com.os.tid.forgerock.openam.models.AddDeviceOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * This node invokes the Check Activation Status Service API, in order to checks the status of a pending activation of a device.
 *
 */
@Node.Metadata( outcomeProvider = OS_Auth_AddDeviceNode.OSTIDAddDeviceOutcomeProvider.class,
                configClass = OS_Auth_AddDeviceNode.Config.class,
                tags = {"OneSpan", "mfa"})
public class OS_Auth_AddDeviceNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_AddDeviceNode";
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OS TID Check Activate Node.
     */
    public interface Config {
    }

    @Inject
    public OS_Auth_AddDeviceNode(@Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OS_Auth_AddDeviceNode started");
        JsonValue sharedState = context.sharedState;
        String tenantName = serviceConfig.tenantNameToLowerCase();
        String environment = serviceConfig.environment().name();

        JsonValue registration_id = sharedState.get(Constants.OSTID_REGISTRATION_ID);
        JsonValue device_code = sharedState.get(Constants.OSTID_DEVICE_CODE);

        if(CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                registration_id,
                device_code
        ))){
            logger.debug("OS_Auth_AddDeviceNode has missing data!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan OCA Add Device call!");
            return goTo(OS_Auth_AddDeviceNode.AddDeviceOutcome.error)
                    .replaceSharedState(sharedState)
                    .build();
        }else{
            String deviceCodeJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_ADD_DEVICE,
                    device_code.asString()                                //param1
            );

            try {
                String url = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_ADAPTIVE_ADD_DEVICE,registration_id.asString());
                HttpEntity httpEntity = RestUtils.doPostJSON(url, deviceCodeJSON);
                JSONObject responseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()) {
                    AddDeviceOutput addDeviceOutput = JSON.toJavaObject(responseJSON, AddDeviceOutput.class);
                    sharedState.put(Constants.OSTID_CRONTO_MSG, addDeviceOutput.getActivationMessage2());

                    return goTo(AddDeviceOutcome.success)
                            .replaceSharedState(sharedState)
                            .build();
                }else{
                    String error = responseJSON.getString("error");
                    String message = responseJSON.getString("message") + StringUtils.getAPIEndpoint(tenantName, environment) + url + " : " + deviceCodeJSON;

                    String log_correction_id = httpEntity.getLog_correlation_id();

                    if(Stream.of(message, error, log_correction_id).anyMatch(Objects::isNull)){
                        throw new NodeProcessException(JSON.toJSONString(responseJSON));
                    }else {
                        JSONArray validationErrors = responseJSON.getJSONArray("validationErrors");
                        if(validationErrors != null && validationErrors.size() > 0 && validationErrors.getJSONObject(0).getString("message") != null){
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, StringUtils.getErrorMsgWithValidation2(message,error,log_correction_id,validationErrors.getJSONObject(0).getString("message")));         //error return from IAA server
                        }else{
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, StringUtils.getErrorMsgWithoutValidation2(message,error,log_correction_id));         //error return from IAA server
                        }
                        return goTo(AddDeviceOutcome.error)
                                .replaceSharedState(sharedState)
                                .build();
                    }
                }
            } catch (IOException | NodeProcessException e) {
                logger.debug("OS_Auth_AddDeviceNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan OCA Add Device process: " + e.getMessage());     //general error msg
                return goTo(AddDeviceOutcome.error)
                        .replaceSharedState(sharedState)
                        .build();
            }

        }
    }



    private Action.ActionBuilder goTo(AddDeviceOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum AddDeviceOutcome{
        success,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDAddDeviceOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_AddDeviceNode.BUNDLE,
                    OSTIDAddDeviceOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(AddDeviceOutcome.success.name(), bundle.getString("successOutcome")),
                    new Outcome(AddDeviceOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

