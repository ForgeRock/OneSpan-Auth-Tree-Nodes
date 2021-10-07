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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.SMSException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 *
 *
 */
@Node.Metadata( outcomeProvider = OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_OutcomeProvider.class,
                configClass = OSTID_DEMO_StoreCommandNode.Config.class,
                tags = {"OneSpan", "mfa"})
public class OSTID_DEMO_StoreCommandNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTID_DEMO_StoreCommandNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTID_DEMO_StoreCommandNode.Config config;
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OSTID_DEMO_ErrorDisplayNode.
     */
    public interface Config {

        /**
         *
         * @return
         */
        @Attribute(order = 100)
        default String javascript() {
            return "";
        }

        /**
         *
         * @return
         */
        @Attribute(order = 200)
        default Map<String, String> placeholderMap() {return Collections.emptyMap(); }

    }



    @Inject
    public OSTID_DEMO_StoreCommandNode(@Assisted OSTID_DEMO_StoreCommandNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }
    @Override
    public Action process(TreeContext context){
        logger.debug("OSTID_DEMO_BackCommandsNode started");
        JsonValue sharedState = context.sharedState;
        String tenantName = serviceConfig.tenantNameToLowerCase();

        JsonValue ostid_sessionid = sharedState.get(Constants.OSTID_SESSIONID);
        JsonValue ostid_irm_response = sharedState.get(Constants.OSTID_IRM_RESPONSE);
        JsonValue ostid_command = sharedState.get(Constants.OSTID_COMMAND);
        String requestId = sharedState.get(Constants.OSTID_REQUEST_ID).isString() ? sharedState.get(Constants.OSTID_REQUEST_ID).asString() : ""; //temporary, the request ID is not mandatory

//        if (CollectionsUtils.hasAnyNullValues(ImmutableList.of(
//                ostid_sessionid,
//                ostid_irm_response,
//                ostid_command
//        ))
//        ) {  //collected data is not intact
//            logger.debug("OSTID_DEMO_BackCommandsNode exception: Oopts, there's missing data for OneSpan TID Back Command Store Process!");
//            sharedState.put(Constants.OSTID_ERROR_MESSAGE, "Oopts, there's missing data for OneSpan TID Back Command Store Process!");
//            return goTo(OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Error)
//                    .replaceSharedState(sharedState)
//                    .build();
//        } else {
            try {
                //build payload
                String demo_cmd_payload = String.format(Constants.OSTID_JSON_DEMO_COMMANDS, ostid_command.asString(), ostid_irm_response.asInteger()+"", ostid_sessionid.asString());

                //build back command API URL
                Map<String, String> placeholders = new HashMap<String, String>() {{
                    put("tenantName", tenantName);
                    put("sessionIdentifier", StringUtils.hexToString(ostid_sessionid.asString()));
                    put("sessionID", ostid_sessionid.asString());
                    put("requestID", requestId);
                    put("username", sharedState.get(Constants.OSTID_DEFAULT_USERNAME).isString() ? sharedState.get(Constants.OSTID_DEFAULT_USERNAME).asString() : "username");
                    put("hexRequestID", StringUtils.stringToHex(requestId));
                }};

                for (Map.Entry<String, String> entry : config.placeholderMap().entrySet()) {
                    placeholders.put(entry.getKey(),sharedState.get(entry.getValue()).isString() ? sharedState.get(entry.getValue()).asString() : entry.getValue());
                }

                String commandURL = config.javascript();
                StrSubstitutor sub = new StrSubstitutor(placeholders, "{", "}");
                String commandURLFinal = sub.replace(commandURL);

                HttpEntity httpEntity = RestUtils.doPostJSONWithoutResponse(commandURLFinal, demo_cmd_payload);

                if (httpEntity.isSuccess()) {
                    return goTo(OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Success)
                            .replaceSharedState(sharedState)
                            .build();
                } else {
                    throw new NodeProcessException(httpEntity.getResponseJSON().toJSONString());
                }
            } catch (Exception e) {
                logger.debug("OSTID_DEMO_BackCommandsNode exception: " + ExceptionUtils.getStackTrace(e));
                sharedState.put(Constants.OSTID_ERROR_MESSAGE, "Fail to Store Command in backoffice!");
                return goTo(OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }
//        }

    }

    private Action.ActionBuilder goTo(OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome outcome) {
        return Action.goTo(outcome.name());
    }
    public enum OSTID_DEMO_StoreCommandNode_Outcome {
        Success, Error
    }
    /**
     * Defines the possible outcomes.
     */
    public static class OSTID_DEMO_StoreCommandNode_OutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTID_DEMO_StoreCommandNode.BUNDLE,
                    OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Success.name(), bundle.getString("successOutcome")),
                    new Outcome(OSTID_DEMO_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }

}
