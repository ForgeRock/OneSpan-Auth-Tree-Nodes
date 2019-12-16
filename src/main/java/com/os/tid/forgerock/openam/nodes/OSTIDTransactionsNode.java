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


@Node.Metadata(outcomeProvider = OSTIDTransactionsNode.OSTIDTransactionsOutcomeProvider.class,
        configClass = OSTIDTransactionsNode.Config.class)
public class OSTIDTransactionsNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDTransactionsNode.Config config;
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTIDTransactionsNode";
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
         *
         * @return
         */
        @Attribute(order = 400)
        default NotificationsActivated notificationsActivated() {
            return NotificationsActivated.Default;
        }
        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 500)
        default int transactionExpiry() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 600)
        default String accountRefInSharedData() {
            return Constants.OSTID_DEFAULT_ACCOUNTREF;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 700)
        default String amountInSharedData() {
            return Constants.OSTID_DEFAULT_AMOUNT;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 800)
        default String currencyInSharedData() {
            return Constants.OSTID_DEFAULT_CURRENCY;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 900)
        default String transactionTypeInSharedData() {
            return Constants.OSTID_DEFAULT_TRANSACTIONTYPE;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 1000)
        default String creditorIBANInSharedData() {
            return Constants.OSTID_DEFAULT_CREDITORIBAN;
        }
        /**
         *
         * @return
         */
        @Attribute(order = 1100)
        default String creditorNameInSharedData() {
            return Constants.OSTID_DEFAULT_CREDITORNAME;
        }




        /**
         * Configurable attributes in request JSON payload
         *
         * @return
         */
        @Attribute(order = 1200)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

    }


    @Inject
    public OSTIDTransactionsNode(@Assisted OSTIDTransactionsNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OSTIDTransactionsNode started");
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
        JsonValue accountRefJsonValue = sharedState.get(config.accountRefInSharedData());
        JsonValue amountJsonValue = sharedState.get(config.amountInSharedData());
        JsonValue currencyJsonValue = sharedState.get(config.currencyInSharedData());
        JsonValue transactionTypeJsonValue = sharedState.get(config.transactionTypeInSharedData());
        JsonValue creditorIBANJsonValue = sharedState.get(config.creditorIBANInSharedData());
        JsonValue creditorNameJsonValue = sharedState.get(config.creditorNameInSharedData());
        JsonValue cddcJsonJsonValue = sharedState.get(Constants.OSTID_CDDC_JSON);
        JsonValue cddcHashJsonValue = sharedState.get(Constants.OSTID_CDDC_HASH);
        JsonValue cddcIpJsonValue = sharedState.get(Constants.OSTID_CDDC_IP);

        if( !passwordInclude || !allOptionalFieldsIncluded ||
            CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                    usernameJsonValue,
                    accountRefJsonValue,
                    amountJsonValue,
                    currencyJsonValue,
                    transactionTypeJsonValue,
                    creditorIBANJsonValue,
                    creditorNameJsonValue,
                    cddcJsonJsonValue,
                    cddcHashJsonValue,
                    cddcIpJsonValue
            ))
        ){
            //todo, custom outcome for exceptions
            logger.debug("OSTIDTransactionsNode exception: has missing data!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan TID Transactions Process!");
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

            String transactionJSON = String.format(Constants.OSTID_JSON_TRANSACTIONS,
                    optionalAttributesStringBuilder.toString(),                   //param1
                    accountRefJsonValue.asString(),                               //param2
                    amountJsonValue.asString(),                                   //param3
                    currencyJsonValue.asString(),                                 //param4
                    transactionTypeJsonValue.asString(),                          //param5
                    creditorIBANJsonValue.asString(),                             //param6
                    creditorNameJsonValue.asString(),                             //param7
                    usernameJsonValue.asString(),                                 //param8
                    sessionID,                                                    //param9
                    cddcJsonJsonValue.asString(),                                 //param10
                    cddcHashJsonValue.asString(),                                 //param11
                    cddcIpJsonValue.asString(),                                   //param12
                    notificationsActivatedJSON,                                   //param13
                    passKeyJSON                                                   //param14
            );
            try {
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName,environment) + Constants.OSTID_API_TRANSACTION, transactionJSON);
                JSONObject eventValidationResponseJSON = httpEntity.getResponseJSON();
                if(httpEntity.isSuccess()) {
                    EventValidationOutput eventValidationOutput = JSON.toJavaObject(eventValidationResponseJSON, EventValidationOutput.class);

                    sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(config.transactionExpiry()));
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
                    sharedState.put(Constants.OSTID_ERROR_MESSAGE,message);
                    return goTo(EventValidationOutcome.Error)
                            .replaceSharedState(sharedState)
                            .build();
                }
            } catch (IOException | NodeProcessException e) {
                logger.debug("OSTIDTransactionsNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID Event Validation process: Fail to invoke Event Validation!");
                return goTo(EventValidationOutcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }
        }
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
    private Action.ActionBuilder goTo(OSTIDTransactionsNode.EventValidationOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTIDTransactionsOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTIDTransactionsNode.BUNDLE,
                    OSTIDTransactionsOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.Challenge.name(), bundle.getString("challengeOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeSMS.name(), bundle.getString("challengeSMSOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeDevice.name(), bundle.getString("challengeDeviceOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeEmail.name(), bundle.getString("challengeEmailOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeCronto.name(), bundle.getString("challengeCrontoOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeNoPIN.name(), bundle.getString("challengeNoPINOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengePIN.name(), bundle.getString("challengePINOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeFingerprint.name(), bundle.getString("challengeFingerprintOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.ChallengeFace.name(), bundle.getString("challengeFaceOutcome")),
                    new Outcome(OSTIDTransactionsNode.EventValidationOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }

}
