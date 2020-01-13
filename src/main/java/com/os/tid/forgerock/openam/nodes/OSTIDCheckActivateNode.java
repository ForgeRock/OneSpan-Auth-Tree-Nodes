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
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.DateUtils;
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
import org.forgerock.openam.auth.node.api.OutcomeProvider;

import javax.security.auth.callback.Callback;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This node invokes the Check Activation Status Service API, in order to checks the status of a pending activation of a device.
 *
 */
@Node.Metadata(outcomeProvider = OSTIDCheckActivateNode.OSTIDCheckActivateOutcomeProvider.class,
            configClass = OSTIDCheckActivateNode.Config.class)
public class OSTIDCheckActivateNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTIDCheckActivateNode";
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OS TID Check Activate Node.
     */
    public interface Config {
    }

    @Inject
    public OSTIDCheckActivateNode(@Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OSTIDCheckActivateNode started");
        JsonValue sharedState = context.sharedState;
        String tenantName = serviceConfig.tenantName();
        String environment = serviceConfig.environment();

        //1. go to next
        JsonValue ostid_cronto_status = sharedState.get(Constants.OSTID_CRONTO_STATUS);
        if(ostid_cronto_status.isString()){
            ActivationStatusOutcome ostid_cronto_status_enum = ActivationStatusOutcome.valueOf(ostid_cronto_status.asString());
            sharedState.remove(Constants.OSTID_CRONTO_STATUS);
            return goTo(ostid_cronto_status_enum)
                    .replaceSharedState(sharedState)
                    .build();
        }

        //2. call API and send script to remove visual code
        JsonValue usernameJsonValue = sharedState.get(sharedState.get(Constants.OSTID_USERNAME_IN_SHARED_STATE).asString());
        JsonValue eventExpiryJsonValue = sharedState.get(Constants.OSTID_EVENT_EXPIRY_DATE);
        ActivationStatusOutcome activationStatusEnum;
        if (!usernameJsonValue.isString() || usernameJsonValue.asString().isEmpty()) {
            activationStatusEnum = ActivationStatusOutcome.error;
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID check activation process: username is missing!");
        } else if(DateUtils.hasExpired(eventExpiryJsonValue.asString())){
            activationStatusEnum = ActivationStatusOutcome.timeout;
        } else {
            String checkActivationJSON = String.format(Constants.OSTID_JSON_CHECK_ACTIVATION,
                    usernameJsonValue.asString(),                            //param1
                    Constants.OSTID_DEFAULT_CHECK_ACTIVATION_TIMEOUT         //param2
            );
            try {
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName,environment) + Constants.OSTID_API_CHECK_ACTIVATION, checkActivationJSON);
                JSONObject checkActivationResponseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()){
                    String activationStatus = checkActivationResponseJSON.getString(Constants.OSTID_RESPONSE_CHECK_ACTIVATION_STATUS);
                    activationStatusEnum = ActivationStatusOutcome.valueOf(activationStatus);
                }else{
                    String message = checkActivationResponseJSON.getString("message");
                    if(message == null){
                        throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(checkActivationResponseJSON));
                    }
                    activationStatusEnum = ActivationStatusOutcome.error;
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE,message);
                }
            } catch (Exception e) {
                logger.debug("OSTIDCheckActivateNode exception: " + e.getMessage());
                activationStatusEnum = ActivationStatusOutcome.error;
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID check activation process: Fail to check user activation status!");
            }
        }

        switch (activationStatusEnum) {
            case pending:
                return goTo(ActivationStatusOutcome.pending).build();
            case activated:
                if(sharedState.get(Constants.OSTID_CRONTO_PUSH_JS).isNull()){
                    return goTo(ActivationStatusOutcome.activated).build();
                }else {
                    sharedState.put(Constants.OSTID_CRONTO_STATUS, ActivationStatusOutcome.activated.name());
                    return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
                }
            case error:
                if(sharedState.get(Constants.OSTID_CRONTO_PUSH_JS).isNull()){
                    return goTo(ActivationStatusOutcome.error).replaceSharedState(sharedState).build();
                }else {
                    sharedState.put(Constants.OSTID_CRONTO_STATUS, ActivationStatusOutcome.error.name());
                    return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
                }
            case timeout:
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID check Activation status process: Fail to check Activation status!");
                if(sharedState.get(Constants.OSTID_CRONTO_PUSH_JS).isNull()){
                    return goTo(ActivationStatusOutcome.timeout).replaceSharedState(sharedState).build();
                }else {
                    sharedState.put(Constants.OSTID_CRONTO_STATUS, ActivationStatusOutcome.timeout.name());
                    return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
                }
            case unknown:
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID check Activation status process: status unknown!");
                if(sharedState.get(Constants.OSTID_CRONTO_PUSH_JS).isNull()){
                    return goTo(ActivationStatusOutcome.timeout).replaceSharedState(sharedState).build();
                }else {
                    sharedState.put(Constants.OSTID_CRONTO_STATUS, ActivationStatusOutcome.unknown.name());
                    return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
                }
            default:
                return goTo(ActivationStatusOutcome.pending).build();
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

    private Action.ActionBuilder goTo(ActivationStatusOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum ActivationStatusOutcome{
        pending,
        activated,
        timeout,
        unknown,
        error
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDCheckActivateOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTIDCheckActivateNode.BUNDLE,
                    OSTIDCheckActivateOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(ActivationStatusOutcome.pending.name(), bundle.getString("pendingOutcome")),
                    new Outcome(ActivationStatusOutcome.activated.name(), bundle.getString("activatedOutcome")),
                    new Outcome(ActivationStatusOutcome.timeout.name(), bundle.getString("timeoutOutcome")),
                    new Outcome(ActivationStatusOutcome.unknown.name(), bundle.getString("unknownOutcome")),
                    new Outcome(ActivationStatusOutcome.error.name(), bundle.getString("errorOutcome")));
        }
    }
}

