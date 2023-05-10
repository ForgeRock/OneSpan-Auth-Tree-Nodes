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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.text.StrSubstitutor;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import com.os.tid.forgerock.openam.nodes.OS_Risk_InsertTransactionNode.RiskTransactionOutcome;
import com.os.tid.forgerock.openam.utils.RestUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

@Node.Metadata(outcomeProvider = OS_Sample_StoreCommandNode.OSTID_DEMO_StoreCommandNode_OutcomeProvider.class,
        configClass = OS_Sample_StoreCommandNode.Config.class,
        tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Sample_StoreCommandNode implements Node {
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Sample_StoreCommandNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OS_Sample_StoreCommandNode.Config config;
    private final OSConfigurationsService serviceConfig;
    private static final String loggerPrefix = "[OneSpan Sample Store Command][Marketplace] ";

    /**
     * Configuration for the OS_Sample_StoreCommandNode.
     */
    public interface Config {
        /**
         *
         */
        @Attribute(order = 100)
        default String javascript() {
            return "";
        }

        /**
         *
         */
        @Attribute(order = 200)
        default Map<String, String> placeholderMap() {
            return Collections.emptyMap();
        }
        
        
        /**
        *
        */
       @Attribute(order = 300)
       default Map<String, String> requestHeaders() {
           return Collections.emptyMap();
       }
       
       /**
       *
       */
       @Attribute(order = 400, validators = RequiredValueValidator.class)
       default HttpMethod httpmethod() {
           return HttpMethod.POST;
       }
        
    }

    @Inject
    public OS_Sample_StoreCommandNode(@Assisted OS_Sample_StoreCommandNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context){
    	try {
	        logger.debug(loggerPrefix + "OSTID_DEMO_BackCommandsNode started");
	        JsonValue sharedState = context.sharedState;
	        String tenantName = serviceConfig.tenantName().toLowerCase();
	
	        JsonValue ostid_sessionid = sharedState.get(Constants.OSTID_SESSIONID);
	        JsonValue ostid_irm_response = sharedState.get(Constants.OSTID_IRM_RESPONSE);
	        JsonValue ostid_command = sharedState.get(Constants.OSTID_COMMAND);
	        String requestId = sharedState.get(Constants.OSTID_REQUEST_ID).isString() ? sharedState.get(Constants.OSTID_REQUEST_ID).asString() : ""; //temporary, the request ID is not mandatory
            //build payload
            String demo_cmd_payload = String.format(Constants.OSTID_JSON_DEMO_COMMANDS, ostid_command.asString(), ostid_irm_response.asInteger() + "", ostid_sessionid.asString());

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
                placeholders.put(entry.getKey(), sharedState.get(entry.getValue()).isString() ? sharedState.get(entry.getValue()).asString() : entry.getValue());
            }

            String commandURL = config.javascript();
            StrSubstitutor sub = new StrSubstitutor(placeholders, "{", "}");
            String commandURLFinal = sub.replace(commandURL);

            HttpEntity httpEntity = RestUtils.doHttpRequestWithoutResponse(commandURLFinal, demo_cmd_payload,config.httpmethod().name(),config.requestHeaders());

            if (httpEntity.isSuccess()) {
                return goTo(OS_Sample_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Success)
                        .replaceSharedState(sharedState)
                        .build();
            } else {
                throw new NodeProcessException(httpEntity.getResponseJSON().toJSONString());
            }
    	}catch (Exception ex) {
	   		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			JsonValue sharedState = context.sharedState;
		    JsonValue transientState = context.transientState;
			sharedState.put("OSTID_DEMO_BackCommandsNode Exception", new Date() + ": " + ex.getMessage());
			sharedState.put(Constants.OSTID_ERROR_MESSAGE, "Fail to Store Command in backoffice: " + ex.getMessage());
			return goTo(OSTID_DEMO_StoreCommandNode_Outcome.Error)
                     .replaceSharedState(sharedState)
                     .replaceTransientState(transientState)
                     .build();	
	    }
    }

    private Action.ActionBuilder goTo(OS_Sample_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome outcome) {
        return Action.goTo(outcome.name());
    }

    public enum OSTID_DEMO_StoreCommandNode_Outcome {
        Success, Error
    }
    
    public enum HttpMethod {
    	POST,PUT,GET,DELETE,PATCH
    }

    /**
     * Defines the possible outcomes.
     */
    public static class OSTID_DEMO_StoreCommandNode_OutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Sample_StoreCommandNode.BUNDLE,
                    OS_Sample_StoreCommandNode.OSTID_DEMO_StoreCommandNode_OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Sample_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Success.name(), bundle.getString("successOutcome")),
                    new Outcome(OS_Sample_StoreCommandNode.OSTID_DEMO_StoreCommandNode_Outcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
