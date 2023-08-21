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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.utils.StringUtils;

@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = OS_Sample_ErrorDisplayNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Sample_ErrorDisplayNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String loggerPrefix = "[OneSpan Sample Error Display][Marketplace] ";

    /**
     * Configuration for the OS_Sample_ErrorDisplayNode.
     */
    public interface Config {
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
    	try {
	        logger.debug(loggerPrefix + "OS_Sample_ErrorDisplayNode started");
	        JsonValue sharedState = context.sharedState;
	        JsonValue ostid_error_msg = sharedState.get(Constants.OSTID_ERROR_MESSAGE);
	        if(ostid_error_msg.isString()){
	            String[] split = ostid_error_msg.asString().split("<br />");
	            List<TextOutputCallback> outputCallbackList = new ArrayList<>();
	            for (String errorMsg: split) {
	                if(!StringUtils.isEmpty(errorMsg)) {
	                    TextOutputCallback errorTextOutputCallback;
	                    errorTextOutputCallback = new TextOutputCallback(2, errorMsg);
	                    outputCallbackList.add(errorTextOutputCallback);
	                }
	            }
	            sharedState.remove(Constants.OSTID_ERROR_MESSAGE);
	            return Action.send(outputCallbackList)
	                    .replaceSharedState(sharedState)
	                    .build();
	        }else{
	            return goToNext().build();
	        }
    	}catch (Exception ex) {
    		String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared("OS_Sample_ErrorDisplayNode Exception", new Date() + ": " + stackTrace)
									 .putShared(Constants.OSTID_ERROR_MESSAGE, "OneSpan Sample Error Display: " + stackTrace);
			throw new NodeProcessException(ex.getMessage());
	    }
    }
    
}
