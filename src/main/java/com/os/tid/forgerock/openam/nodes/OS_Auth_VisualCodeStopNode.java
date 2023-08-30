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
import java.util.List;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.os.tid.forgerock.openam.config.Constants;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;


/**
 * This node hides the visual code from UI
 */
@Node.Metadata( outcomeProvider = OS_Auth_VisualCodeStopNode.OSAuthVisualCodeStopOutcomeProvider.class,
                configClass = OS_Auth_VisualCodeStopNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Auth_VisualCodeStopNode implements Node {
    private final Logger logger = LoggerFactory.getLogger(OS_Auth_VisualCodeStopNode.class);
    private static final String loggerPrefix = "[OneSpan Auth Hide Visual Code]" + OSAuthNodePlugin.logAppender;
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Auth_VisualCodeStopNode";

    /**
     * Configuration for the OneSpan Auth Stop Visual Code Node.
     */
    public interface Config {
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
    	try {
	        logger.debug(loggerPrefix + "OS_Auth_VisualCodeNode started");
	        NodeState ns = context.getStateFor(this);
	
	        if (ns.get("os_tid_visualcodestop").isBoolean()) {
	        	return goTo(VisualCodeStopOutcome.Next).build();
	        } else {
	            ns.putShared("os_tid_visualcodestop",true);
	            return Action.send(getStopCrontoCallback()).build();
	        }
    	}catch (Exception ex) {
    		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "OS_Auth_VisualCodeStopNode Exception", new Date() + ": " + stackTrace)
									 .putShared(loggerPrefix + Constants.OSTID_ERROR_MESSAGE, "OneSpan Auth Stop Visual Code Node: " + stackTrace);
			return goTo(VisualCodeStopOutcome.Error).build();
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
    
    public enum VisualCodeStopOutcome {
        Next, Error
    }
    
    private Action.ActionBuilder goTo(OS_Auth_VisualCodeStopNode.VisualCodeStopOutcome outcome) {
        return Action.goTo(outcome.name());
    }
    
    /**
     * Defines the possible outcomes.
     */
    public static class OSAuthVisualCodeStopOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(OS_Auth_VisualCodeStopNode.BUNDLE,
            		OS_Auth_VisualCodeStopNode.OSAuthVisualCodeStopOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(OS_Auth_VisualCodeStopNode.VisualCodeStopOutcome.Next.name(), bundle.getString("nextOutcome")),
                    new Outcome(OS_Auth_VisualCodeStopNode.VisualCodeStopOutcome.Error.name(), bundle.getString("errorOutcome"))
            );
        }
    }
}
