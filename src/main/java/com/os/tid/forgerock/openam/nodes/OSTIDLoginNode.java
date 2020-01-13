package com.os.tid.forgerock.openam.nodes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.EventValidationOutput;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
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

/**
 *
 * This node invokes the Login Service API, in order to validate a login request against the Risk Management Service and the Authentication Service, and returns the results of the authentication attempt.
 */
@Node.Metadata(outcomeProvider = OSTIDLoginNode.OSTIDLoginOutcomeProvider.class,
        configClass = OSTIDLoginNode.Config.class)
public class OSTIDLoginNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDLoginNode.Config config;
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTIDLoginNode";
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OneSpan TID Event Validation Node.
     */
    public interface Config {
        /**
         *
         * @return
         */
        @Attribute(order = 100)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 200)
        default boolean passKeyRequired() {
            return false;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 300)
        default String passwordInTransientState() {
            return Constants.OSTID_DEFAULT_PASSKEY;
        }

        /**
         * Configurable attributes in request JSON payload
         *
         * @return
         */
        @Attribute(order = 400)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

        /**
         *
         * @return
         */
        @Attribute(order = 500)
        default NotificationsActivated notificationsActivated() {
            return NotificationsActivated.Default;
        }

        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 600)
        default int loginExpiry() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }

        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 700)
        default VisualCodeMessageOptions visualCodeMessageOptions() {
            return VisualCodeMessageOptions.SessionId;
        }
    }

    @Inject
    public OSTIDLoginNode(@Assisted OSTIDLoginNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OSTIDLoginNode started");
        JsonValue sharedState = context.sharedState;
        JsonValue transientState = context.transientState;
        String tenantName = serviceConfig.tenantName();
        String environment = serviceConfig.environment();

        boolean passwordInclude = true;
        String passKeyJSON = "";
        if(config.passKeyRequired()){
            JsonValue passwordJsonValue = transientState.get(config.passwordInTransientState());
            passwordInclude = passwordJsonValue.isString() && !passwordJsonValue.asString().isEmpty();
            passKeyJSON = String.format(Constants.OSTID_JSON_PASSKEY,passwordJsonValue.asString());
        }

        boolean allOptionalFieldsIncluded = true;
        StringBuilder optionalAttributesStringBuilder = new StringBuilder(1000);
        Map<String, String> optionalAttributesMap = config.optionalAttributes();
        for (Map.Entry<String, String> entrySet : optionalAttributesMap.entrySet()) {
            JsonValue jsonValue = sharedState.get(entrySet.getKey());
            if(jsonValue.isString()){
                optionalAttributesStringBuilder.append("\"").append(entrySet.getValue()).append("\":\"").append(jsonValue.asString()).append("\",");
            }else{
                allOptionalFieldsIncluded = false;
            }
        }

        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
        JsonValue cddcJsonJsonValue = sharedState.get(Constants.OSTID_CDDC_JSON);
        JsonValue cddcHashJsonValue = sharedState.get(Constants.OSTID_CDDC_HASH);
        JsonValue cddcIpJsonValue = sharedState.get(Constants.OSTID_CDDC_IP);

        if( !passwordInclude || !allOptionalFieldsIncluded ||
            CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                    usernameJsonValue,
                    cddcJsonJsonValue,
                    cddcHashJsonValue,
                    cddcIpJsonValue
            ))
        ){
            logger.debug("OSTIDLoginNode has missing data!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan TID Login Process!");
            return goTo(LoginOutcome.Error)
                    .replaceSharedState(sharedState)
                    .build();
        }else{
            String sessionID = sharedState.get(Constants.OSTID_SESSIONID).isString() ? sharedState.get(Constants.OSTID_SESSIONID).asString() : StringUtils.stringToHex(UUID.randomUUID().toString());

            String notificationsActivatedJSON = "";
            switch(config.notificationsActivated()){
                case Yes:
                    notificationsActivatedJSON = String.format(Constants.OSTID_JSON_ISNOTIFIED,Boolean.toString(true));
                    break;
                case No:
                    notificationsActivatedJSON = String.format(Constants.OSTID_JSON_ISNOTIFIED,Boolean.toString(false));
                    break;
                case Default:
                    break;
            }

            String loginJSON = String.format(Constants.OSTID_JSON_LOGIN,
                    passKeyJSON,                                //param1
                    notificationsActivatedJSON,                 //param2
                    usernameJsonValue.asString(),               //param3
                    cddcIpJsonValue.asString(),                 //param4
                    cddcHashJsonValue.asString(),               //param5
                    cddcJsonJsonValue.asString(),               //param6
                    sessionID,                                  //param7
                    tenantName,                                 //param8
                    optionalAttributesStringBuilder.toString()  //param9
            );

            try {
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName,environment) + Constants.OSTID_API_LOGIN, loginJSON);
                JSONObject loginResponseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()) {
                    EventValidationOutput eventValidationOutput = JSON.toJavaObject(loginResponseJSON, EventValidationOutput.class);

                    sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.loginExpiry()));
                    sharedState.put(Constants.OSTID_SESSIONID,sessionID);
                    sharedState.put(Constants.OSTID_REQUEST_ID,eventValidationOutput.getRequestID());
                    sharedState.put(Constants.OSTID_IRM_RESPONSE,eventValidationOutput.getIrmResponse());
                    sharedState.put(Constants.OSTID_COMMAND,eventValidationOutput.getCommand());

                    LoginOutcome loginOutcome = LoginOutcome.Error;
                    int irmResponse = Integer.parseInt(eventValidationOutput.getIrmResponse());
                    if(irmResponse == 0){
                        loginOutcome = LoginOutcome.Accept;
                    }else if(irmResponse == 1){
                        loginOutcome = LoginOutcome.Decline;
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan TID Login process: Request been declined!");
                    }else if(Constants.OSTID_API_CHALLANGE_MAP.containsKey(irmResponse)){
                        loginOutcome = LoginOutcome.StepUp;
                    }

                    String crontoMsg = "";
                    switch(config.visualCodeMessageOptions()) {
                        case SessionId:
                            crontoMsg = sessionID;
                            break;
                        case RequestId:
                            crontoMsg = StringUtils.stringToHex(eventValidationOutput.getRequestID());
                            break;
                        default:
                            crontoMsg = sessionID;
                    }

                    sharedState.put(Constants.OSTID_CRONTO_MSG, crontoMsg);

                    return goTo(loginOutcome)
                            .replaceSharedState(sharedState)
                            .build();
                }else{
                    String message = loginResponseJSON.getString("message");
                    if(message == null){
                        throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(loginResponseJSON));
                    }else {
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE, message);
                        return goTo(LoginOutcome.Error)
                                .replaceSharedState(sharedState)
                                .build();
                    }
                }
            } catch (IOException | NodeProcessException e) {
                logger.debug("OSTIDLoginNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID Login process: Fail to login!");
                return goTo(LoginOutcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }
        }
    }


    public enum NotificationsActivated{
        Yes,No,Default
    }

    public enum VisualCodeMessageOptions{
        SessionId,RequestId
    }

    public enum LoginOutcome {
        Accept,
        Decline,
        StepUp,
        Error
    }

    private Action.ActionBuilder goTo(LoginOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDLoginOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTIDLoginNode.BUNDLE,
                    OSTIDLoginOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(LoginOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(LoginOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(LoginOutcome.StepUp.name(), bundle.getString("stepupOutcome")),
                    new Outcome(LoginOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }

}
