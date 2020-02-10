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

import com.os.tid.forgerock.openam.config.Constants;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.TextOutputCallback;


/**
 *
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = OSTID_DEMO_ErrorDisplayNode.Config.class)
public class OSTID_DEMO_ErrorDisplayNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Configuration for the OSTID_DEMO_ErrorDisplayNode.
     */
    public interface Config {
    }

    @Override
    public Action process(TreeContext context){
        logger.debug("OSTID_DEMO_ErrorDisplayNode started");
        JsonValue sharedState = context.sharedState;
        JsonValue ostid_error_msg = sharedState.get(Constants.OSTID_ERROR_MESSAGE);
        if(ostid_error_msg.isString()){
            TextOutputCallback errorTextOutputCallback = new TextOutputCallback(2,ostid_error_msg.asString());
            sharedState.remove(Constants.OSTID_ERROR_MESSAGE);
            return Action.send(errorTextOutputCallback)
                    .replaceSharedState(sharedState)
                    .build();
        }else{
            return goToNext().build();
        }


    }

}
