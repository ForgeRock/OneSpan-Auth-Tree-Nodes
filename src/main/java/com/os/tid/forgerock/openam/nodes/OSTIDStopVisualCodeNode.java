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

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;


/**
 * This node reads the visual code message from the sharedState and renders it as a visual code, which allows the device integrated with the Mobile Security Suite SDKs to scan with.
 */
@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = OSTIDStopVisualCodeNode.Config.class,
                tags = {"OneSpan", "mfa"})
public class OSTIDStopVisualCodeNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Configuration for the OneSpan TID Stop Visual Code Node.
     */
    public interface Config {
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OSTIDVisualCodeNode started");
        JsonValue sharedState = context.sharedState;

        if (sharedState.get("os_tid_visualcodestop").isBoolean()) {
            sharedState.remove("os_tid_visualcodestop");
            return goToNext().replaceSharedState(sharedState).build();
        } else {
            sharedState.put("os_tid_visualcodestop",true);
            return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
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

}
