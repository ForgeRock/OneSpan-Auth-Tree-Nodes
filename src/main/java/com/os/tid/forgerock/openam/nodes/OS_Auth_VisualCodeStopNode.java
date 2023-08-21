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

import java.util.Date;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.os.tid.forgerock.openam.config.Constants;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;


/**
 * This node hides the visual code from UI
 */
@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = OS_Auth_VisualCodeStopNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_VisualCodeStopNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String loggerPrefix = "[OneSpan Auth Hide Visual Code][Marketplace] ";

    /**
     * Configuration for the OneSpan Auth Stop Visual Code Node.
     */
    public interface Config {
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
    	try {
	        logger.debug(loggerPrefix + "OS_Auth_VisualCodeNode started");
	        JsonValue sharedState = context.sharedState;
	
	        if (sharedState.get("os_tid_visualcodestop").isBoolean()) {
	            return goToNext().replaceSharedState(sharedState).build();
	        } else {
	            sharedState.put("os_tid_visualcodestop",true);
	            return Action.send(getStopCrontoCallback()).replaceSharedState(sharedState).build();
	        }
    	}catch (Exception ex) {
    		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared("OS_Auth_VisualCodeStopNode Exception", new Date() + ": " + stackTrace)
									 .putShared(Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Stop Visual Code Node: " + stackTrace);
			throw new NodeProcessException(ex.getMessage());
	    }
    }

    private Callback getStopCrontoCallback() {
        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(
                        "if (typeof window.CDDC_display == 'function') { " +
                        "    window.CDDC_display(false)();" +
                        "}" +
                        "if(typeof loginHelpers !== 'undefined'){" +
//                        "   document.getElementsByClassName('btn-primary')[0].style.display = 'none';"+
                        "   document.getElementsByClassName('btn-primary')[0].click();"+
                        "}else{" +
//                        "   document.getElementById('loginButton_0').style.display = 'none';"+
                        "   document.getElementById('loginButton_0').click();"+
                        "}"
        );
        return displayScriptCallback;
    }
}
