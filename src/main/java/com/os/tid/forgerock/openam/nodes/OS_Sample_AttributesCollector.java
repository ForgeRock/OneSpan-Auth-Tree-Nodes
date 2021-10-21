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
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import java.util.*;


@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = OS_Sample_AttributesCollector.Config.class,
                tags = {"OneSpan", "mfa", "utilities"})
public class OS_Sample_AttributesCollector extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OS_Sample_AttributesCollector.Config config;

    /**
     * Configuration for the OS_Sample_AttributesCollector.
     */
    public interface Config {
        /**
         * The key name in shared state
         */
        @Attribute(order = 100)
        default Set<String> attributes() {
            return Collections.emptySet();
        }
    }

    @Inject
    public OS_Sample_AttributesCollector(@Assisted OS_Sample_AttributesCollector.Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context){
        logger.debug("OS_Sample_AttributesCollector started");
        JsonValue sharedState = context.sharedState;

        //1. if all attributes have value
        Map<String, String> attrValueMap = new HashMap<>(); //attribute name in sharedState : callback value
        config.attributes().forEach(attr -> attrValueMap.putIfAbsent(attr,null));

        if (context.getCallbacks(NameCallback.class) != null && context.getCallbacks(NameCallback.class).size() >= 0) {
            context.getCallbacks(NameCallback.class).forEach(nameCallback -> attrValueMap.put(nameCallback.getPrompt(), nameCallback.getName()));
        }

        if (!CollectionsUtils.hasAnyNullValues(attrValueMap)) {
            //2. set shared state
            attrValueMap.forEach((key, value) -> sharedState.put(key, value));
            return goToNext()
                    .replaceSharedState(sharedState)
                    .build();
        } else {
            //3. return list of callbacks
            List<Callback> callbackList = new ArrayList<>();
            config.attributes().forEach(key -> callbackList.add(new NameCallback(key,key)));
            return Action.send(callbackList).build();
        }
    }
}
