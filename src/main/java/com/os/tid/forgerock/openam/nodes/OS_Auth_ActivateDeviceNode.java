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
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * This node invokes the Activate Device API, which finalizes the OCA provisioning process.
 *
 */
@Node.Metadata( outcomeProvider = OS_Auth_ActivateDeviceNode.OSTIDActivateDeviceOutcomeProvider.class,
                configClass = OS_Auth_ActivateDeviceNode.Config.class,
                tags = {"OneSpan", "mfa", "basic authentication"})
public class OS_Auth_ActivateDeviceNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_ActivateDeviceNode";
    private final OSConfigurationsService serviceConfig;

    /**
     * Configuration for the OS Auth Activate Device Node.
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
        logger.debug("OS_Auth_ActivateDeviceNode started");
        JsonValue sharedState = context.sharedState;
        String tenantName = serviceConfig.tenantNameToLowerCase();
        String environment = serviceConfig.environment().name();

        JsonValue registration_id = sharedState.get(Constants.OSTID_REGISTRATION_ID);
        JsonValue signature = sharedState.get(Constants.OSTID_SIGNATURE);

        //1. go to next
        JsonValue ostid_cronto_status = sharedState.get(Constants.OSTID_CRONTO_STATUS);
        if(ostid_cronto_status.isString()){
            OSTIDActivateDeviceOutcome ostid_cronto_status_enum = OSTIDActivateDeviceOutcome.valueOf(ostid_cronto_status.asString());
            sharedState.remove(Constants.OSTID_CRONTO_STATUS);
            return goTo(ostid_cronto_status_enum)
                    .replaceSharedState(sharedState)
                    .build();
        }

        //2. invoke API
        if(CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                registration_id,
                signature
        ))){
            logger.debug("OS_Auth_ActivateDeviceNode has missing data!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there are missing data for OneSpan OCA Activate Device call!");
            return goTo(OS_Auth_ActivateDeviceNode.OSTIDActivateDeviceOutcome.error)
                    .replaceSharedState(sharedState)
                    .build();
        }else{
            String activateDeviceJSON = String.format(Constants.OSTID_JSON_ADAPTIVE_ACTIVATE_DEVICE,
                    signature.asString()                                //param1
            );

            try {
                String url = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_ADAPTIVE_ACTIVATE_DEVICE,registration_id.asString());
                HttpEntity httpEntity = RestUtils.doPostJSON(url, activateDeviceJSON);
                JSONObject responseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()) {
                    sharedState.put(Constants.OSTID_CRONTO_STATUS, OSTIDActivateDeviceOutcome.success.name());
                    return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
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
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, StringUtils.getErrorMsgNoRetCodeWithValidation(message,log_correction_id,validationErrors.getJSONObject(0).getString("message"),requestJSON));         //error return from IAA server
                        }else{
                            sharedState.put(Constants.OSTID_ERROR_MESSAGE, StringUtils.getErrorMsgNoRetCodeWithoutValidation(message,log_correction_id,requestJSON));         //error return from IAA server
                        }
                        return goTo(OSTIDActivateDeviceOutcome.error)
                                .replaceSharedState(sharedState)
                                .build();
                    }
                }
            } catch (IOException | NodeProcessException e) {
                logger.debug("OS_Auth_ActivateDeviceNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan OCA Activate Device process: " + e.getMessage());     //general error msg
                return goTo(OSTIDActivateDeviceOutcome.error)
                        .replaceSharedState(sharedState)
                        .build();
            }

        }
    }

    private Callback getStopCrontoCallback() {
        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(
                "document.getElementById('loginButton_0').style.display = 'none';" +
                        "if (CDDC_stop && typeof CDDC_stop === 'function') { " +
                        "    CDDC_stop();" +
                        "}" +
                        "document.getElementById('loginButton_0').click();");
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

