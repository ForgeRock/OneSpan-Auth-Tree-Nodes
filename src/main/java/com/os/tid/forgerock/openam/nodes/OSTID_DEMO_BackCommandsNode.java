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
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
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

import javax.inject.Inject;
import java.util.*;

/**
 *
 *
 */
@Node.Metadata(outcomeProvider = OSTID_DEMO_BackCommandsNode.OSTID_DEMO_BackCommandsNode_OutcomeProvider.class,
            configClass = OSTID_DEMO_BackCommandsNode.Config.class)
public class OSTID_DEMO_BackCommandsNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTID_DEMO_BackCommandsNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OSTID_DEMO_BackCommandsNode.
     */
    public interface Config {
    }

    @Inject
    public OSTID_DEMO_BackCommandsNode(@Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
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
        String tenantName = serviceConfig.tenantName();
        String environment = serviceConfig.environment();

        JsonValue ostid_sessionid = sharedState.get(Constants.OSTID_SESSIONID);
        JsonValue ostid_request_id = sharedState.get(Constants.OSTID_REQUEST_ID);
        JsonValue ostid_irm_response = sharedState.get(Constants.OSTID_IRM_RESPONSE);
        JsonValue ostid_command = sharedState.get(Constants.OSTID_COMMAND);

        if (CollectionsUtils.hasAnyNullValues(ImmutableList.of(
                ostid_sessionid,
                ostid_request_id,
                ostid_irm_response,
                ostid_command
        ))
        ) {  //collected data is not intact
            logger.debug("OSTID_DEMO_BackCommandsNode exception: Oopts, there's missing data for OneSpan TID Back Command Store Process!");
            sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Oopts, there's missing data for OneSpan TID Back Command Store Process!");
            return goTo(Demo_BackCommandsNode_Outcome.Error)
                    .replaceSharedState(sharedState)
                    .build();
        } else {
            try {
                String demo_cmd_payload = String.format(Constants.OSTID_JSON_DEMO_COMMANDS, ostid_command.asString(), ostid_irm_response.asString(),StringUtils.hexToString(ostid_sessionid.asString()));
                HttpEntity httpEntity = RestUtils.doPostJSON(StringUtils.getAPIEndpoint(tenantName, environment) + Constants.OSTID_API_DEMO_COMMANDS, demo_cmd_payload);

                if (httpEntity.isSuccess()) {
                    return goTo(Demo_BackCommandsNode_Outcome.Success)
                            .replaceSharedState(sharedState)
                            .build();
                } else {
                    throw new NodeProcessException(httpEntity.getResponseJSON().toJSONString());
                }
            } catch (Exception e) {
                logger.debug("OSTID_DEMO_BackCommandsNode exception: " + e.getMessage());
                sharedState.put(Constants.OSTID_ERROR_MESSAGE,"Fail to Store Command in backoffice!");
                return goTo(Demo_BackCommandsNode_Outcome.Error)
                        .replaceSharedState(sharedState)
                        .build();
            }
        }
    }

    public enum Demo_BackCommandsNode_Outcome {
        Success,Error
    }
    private Action.ActionBuilder goTo(Demo_BackCommandsNode_Outcome outcome) {
        return Action.goTo(outcome.name());
    }
    /**
     * Defines the possible outcomes.
     */
    public static class OSTID_DEMO_BackCommandsNode_OutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OSTID_DEMO_BackCommandsNode.BUNDLE,
                    OSTID_DEMO_BackCommandsNode_OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(Demo_BackCommandsNode_Outcome.Success.name(), bundle.getString("successOutcome")),
                    new Outcome(Demo_BackCommandsNode_Outcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
