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
 * This node invokes the Transaction Service API, in order to validate a monetary transaction request against the Risk Management Service and the Authentication Service, and returns the result.
 */
@Node.Metadata(outcomeProvider = OSTIDTransactionsNode.OSTIDTransactionsOutcomeProvider.class,
        configClass = OSTIDTransactionsNode.Config.class)
public class OSTIDTransactionsNode implements Node {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDTransactionsNode.Config config;
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTIDTransactionsNode";
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OneSpan TID Transaction Event Node.
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
        default String transactionTypeInSharedData() {
            return Constants.OSTID_DEFAULT_TRANSACTIONTYPE;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 500)
        default String currencyInSharedData() {
            return Constants.OSTID_DEFAULT_CURRENCY;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 600)
        default String amountInSharedData() {
            return Constants.OSTID_DEFAULT_AMOUNT;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 700)
        default String creditorIBANInSharedData() {
            return Constants.OSTID_DEFAULT_CREDITORIBAN;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 800)
        default String accountRefInSharedData() {
            return Constants.OSTID_DEFAULT_ACCOUNTREF;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 900)
        default String creditorNameInSharedData() {
            return Constants.OSTID_DEFAULT_CREDITORNAME;
        }

        /**
         * Configurable attributes in request JSON payload
         *
         * @return
         */
        @Attribute(order = 1000)
        default Map<String, String> optionalAttributes() {
            return Collections.emptyMap();
        }

        /**
         *
         * @return
         */
        @Attribute(order = 1100)
        default NotificationsActivated notificationsActivated() {
            return NotificationsActivated.Default;
        }

        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 1200)
        default int transactionExpiry() {
            return Constants.OSTID_DEFAULT_EVENT_EXPIRY;
        }

        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 1300)
        default VisualCodeMessageOptions visualCodeMessageOptions() {
            return VisualCodeMessageOptions.SessionId;
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
            logger.debug("OSTIDTransactionsNode exception: has missing data!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan TID Transactions Process!");
            return goTo(TransactionOutcome.Error)
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
                    sharedState.put(Constants.OSTID_IRM_RESPONSE,eventValidationOutput.getIrmResponse());
                    sharedState.put(Constants.OSTID_COMMAND,eventValidationOutput.getCommand());

                    TransactionOutcome transactionOutcome = TransactionOutcome.Error;
                    int irmResponse = Integer.parseInt(eventValidationOutput.getIrmResponse());
                    if(irmResponse == 0){
                        transactionOutcome = TransactionOutcome.Accept;
                    }else if(irmResponse == 1){
                        transactionOutcome = TransactionOutcome.Decline;
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE, "OneSpan TID Event Validation process: Request been declined!");
                    }else if(Constants.OSTID_API_CHALLANGE_MAP.containsKey(irmResponse)){
                        transactionOutcome = TransactionOutcome.StepUp;
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

                    return goTo(transactionOutcome)
                            .replaceSharedState(sharedState)
                            .build();
                }else{
                    String message = eventValidationResponseJSON.getString("message");
                    if(message == null){
                        throw new NodeProcessException("Fail to parse response: " + JSON.toJSONString(eventValidationResponseJSON));
                    }else{
                        sharedState.put(Constants.OSTID_ERROR_MESSAGE,message);
                        return goTo(TransactionOutcome.Error)
                                .replaceSharedState(sharedState)
                                .build();
                    }
                }
            } catch (IOException | NodeProcessException e) {
                logger.debug("OSTIDTransactionsNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"OneSpan TID Event Validation process: Fail to invoke Event Validation!");
                return goTo(TransactionOutcome.Error)
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

    public enum TransactionOutcome {
        Accept,
        Decline,
        StepUp,
        Error
    }
    private Action.ActionBuilder goTo(TransactionOutcome outcome) {
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
                    new Outcome(TransactionOutcome.Accept.name(), bundle.getString("acceptOutcome")),
                    new Outcome(TransactionOutcome.Decline.name(), bundle.getString("declineOutcome")),
                    new Outcome(TransactionOutcome.StepUp.name(), bundle.getString("stepupOutcome")),
                    new Outcome(TransactionOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }

}
