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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = OSTID_DEMO_InsertJavaScriptNode.Config.class)
public class OSTID_DEMO_InsertJavaScriptNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTID_DEMO_InsertJavaScriptNode.Config config;

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

    }



    @Inject
    public OSTID_DEMO_InsertJavaScriptNode(@Assisted OSTID_DEMO_InsertJavaScriptNode.Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context){
        logger.debug("OSTID_DEMO_InsertJavaScriptNode started");
        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(config.javascript());
        JsonValue sharedState = context.sharedState;
        JsonValue os_js_insert = sharedState.get("os_js_insert");
        if(os_js_insert.isNotNull() && os_js_insert.asBoolean()){
            return goToNext().build();
        }else{
            sharedState.put("os_js_insert",true);
            return Action.send(displayScriptCallback).replaceSharedState(sharedState).build();
        }

    }

}
