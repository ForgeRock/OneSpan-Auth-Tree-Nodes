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
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;


@Node.Metadata(outcomeProvider = OSTIDEventValidationNode.OSTIDEventValidationOutcomeProvider.class,
        configClass = OSTIDEventValidationNode.Config.class)
public class OSTIDEventValidationNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDEventValidationNode.Config config;
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTIDEventValidationNode";
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
        default EventType eventType() {
            return EventType.LoginAttempt;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 200)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 300)
        default boolean passKeyRequired() {
            return false;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 400)
        default String passwordInTransientState() {
            return Constants.OSTID_DEFAULT_PASSKEY;
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
        default int eventValidationExpiry() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }
    }


    @Inject
    public OSTIDEventValidationNode(@Assisted OSTIDEventValidationNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OSTIDEventValidationNode started");
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

        JsonValue usernameJsonValue = sharedState.get(config.userNameInSharedData());
        JsonValue cddcJsonJsonValue = sharedState.get(Constants.OSTID_CDDC_JSON);
        JsonValue cddcHashJsonValue = sharedState.get(Constants.OSTID_CDDC_HASH);
        JsonValue cddcIpJsonValue = sharedState.get(Constants.OSTID_CDDC_IP);

        if( !passwordInclude ||
            CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                    usernameJsonValue,
                    cddcJsonJsonValue,
                    cddcHashJsonValue,
                    cddcIpJsonValue
            ))
        ){
            //todo, custom outcome for exceptions
            logger.debug("OSTIDEventValidationNode has missing data!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan TID Event Validation Process!");
            return goTo(EventValidationOutcome.Error)
                    .replaceSharedState(sharedState)
                    .build();
        }else{
            String sessionID = sharedState.get(Constants.OSTID_SESSIONID).isString() ? sharedState.get(Constants.OSTID_SESSIONID).asString() : StringUtils.toHex(UUID.randomUUID().toString());

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

            String eventValidationJSON = String.format(Constants.OSTID_JSON_EVENT_VALIDATION,
                    passKeyJSON,                    //param1
                    notificationsActivatedJSON,     //param2
                    config.eventType().name(),      //param3
                    usernameJsonValue.asString(),   //param4
                    cddcIpJsonValue.asString(),     //param5
                    cddcHashJsonValue.asString(),   //param6
                    cddcJsonJsonValue.asString(),   //param7
                    sessionID,                      //param8
                    tenantName                      //param9
            );
            try {
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName,environment) + Constants.OSTID_API_EVENT_VALIDATION, eventValidationJSON);
                JSONObject eventValidationResponseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()) {
                    EventValidationOutput eventValidationOutput = JSON.toJavaObject(eventValidationResponseJSON, EventValidationOutput.class);

                    //todo, put other attrs into shared state
                    sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.eventValidationExpiry()));
                    sharedState.put(Constants.OSTID_SESSIONID,sessionID);
                    sharedState.put(Constants.OSTID_REQUEST_ID,eventValidationOutput.getRequestID());

//                    sharedState.remove(Constants.OSTID_CDDC_JSON);
//                    sharedState.remove(Constants.OSTID_CDDC_HASH);
//                    sharedState.remove(Constants.OSTID_CDDC_IP);

                    int irmResponse = Integer.parseInt(eventValidationOutput.getIrmResponse());
                    EventValidationOutcome eventValidationOutcomeByCode = EventValidationOutcome.getEventValidationOutcomeByCode(irmResponse);

                    if(eventValidationOutcomeByCode == EventValidationOutcome.Decline){
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan TID Event Validation process: Request been declined!");
                    }

                    String crontoValueHex = StringUtils.toHex(eventValidationOutput.getRequestID());
                    sharedState.put(Constants.OSTID_CRONTO_MSG, crontoValueHex);

                    return goTo(eventValidationOutcomeByCode)
                            .replaceSharedState(sharedState)
                            .build();
                }else{
                    String message = eventValidationResponseJSON.getString("message");
                    if(message == null){
                        throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(eventValidationResponseJSON));
                    }
//                        ExceptionOutput exceptionOutput = JSON.toJavaObject(userRegisterResponseJSON, ExceptionOutput.class);
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE,message);
                    return goTo(EventValidationOutcome.Error)
                            .replaceSharedState(sharedState)
                            .build();
                }
                //todo, refactor outcome results
            } catch (IOException | NodeProcessException e) {
                logger.debug("OSTIDEventValidationNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID Event Validation process: Fail to invoke Event Validation!");
                return goTo(EventValidationOutcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }
        }
    }


    public enum EventType{
        LoginAttempt
    }
    public enum NotificationsActivated{
        Yes,No,Default
    }

    //todo, how's this list maps with the IRM's response
    public enum EventValidationOutcome{
        Accept(0),
        Decline(1),
        Challenge(2),
        ChallengeSMS(3),
        ChallengeDevice(4),
        ChallengeEmail(8),
        ChallengeCronto(11),
        ChallengeNoPIN(21),
        ChallengePIN(22),
        ChallengeFingerprint(23),
        ChallengeFace(24),
        Error(Constants.OSTID_DEFAULT_ENUM_ERROR_CODE);

        private final int responseCode;

        EventValidationOutcome(int responseCode) {
            this.responseCode = responseCode;
        }

        public int getResponseCode() {
            return responseCode;
        }

        static EventValidationOutcome getEventValidationOutcomeByCode(int n){
            for (EventValidationOutcome c : values()) {
                if (c.getResponseCode() == n) {
                    return c;
                }
            }
            throw new IllegalArgumentException(String.valueOf(n));
        }

    }
    private Action.ActionBuilder goTo(OSTIDEventValidationNode.EventValidationOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDEventValidationOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTIDEventValidationNode.BUNDLE,
                    OSTIDEventValidationNode.OSTIDEventValidationOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.Challenge.name(), bundle.getString("challengeOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeSMS.name(), bundle.getString("challengeSMSOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeDevice.name(), bundle.getString("challengeDeviceOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeEmail.name(), bundle.getString("challengeEmailOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeCronto.name(), bundle.getString("challengeCrontoOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeNoPIN.name(), bundle.getString("challengeNoPINOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengePIN.name(), bundle.getString("challengePINOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeFingerprint.name(), bundle.getString("challengeFingerprintOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.ChallengeFace.name(), bundle.getString("challengeFaceOutcome")),
                    new Outcome(OSTIDEventValidationNode.EventValidationOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }

}
