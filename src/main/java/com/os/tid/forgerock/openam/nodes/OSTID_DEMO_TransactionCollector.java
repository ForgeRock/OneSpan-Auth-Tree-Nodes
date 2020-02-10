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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.util.*;


/**
 *
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = OSTID_DEMO_TransactionCollector.Config.class)
public class OSTID_DEMO_TransactionCollector extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTID_DEMO_TransactionCollector.Config config;
    private static final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTID_DEMO_TransactionCollector";

    /**
     * Configuration for the OSTID_DEMO_TransactionCollector.
     */
    public interface Config {
        /**
         * Determine whether to include OneSpan IAA user password when attempting to login.
         */
        @Attribute(order = 100)
        default boolean passKeyRequired() {
            return false;
        }

        /**
         * Specify other optional attributes like user email, user phone number, etc.
         */
        @Attribute(order = 200)
        default Set<String> optionalAttributes() {
            return Collections.emptySet();
        }
    }

    @Inject
    public OSTID_DEMO_TransactionCollector(@Assisted OSTID_DEMO_TransactionCollector.Config config)  {
        this.config = config;
    }
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OSTID_DEMO_TransactionCollector started");
        JsonValue sharedState = context.sharedState;
        JsonValue transientState = context.transientState;

        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());

        Map<String, String> attrValueMap = new HashMap<>(); //attribute name in sharedState : callback value
        final Map<String, String> attrMap = new HashMap<String, String>(){{   //label name : attribute name in sharedState
                put(bundle.getString("callback.username"),Constants.OSTID_DEFAULT_USERNAME);
                put(bundle.getString("callback.amount"),Constants.OSTID_DEFAULT_AMOUNT);
                put(bundle.getString("callback.currency"),Constants.OSTID_DEFAULT_CURRENCY);
                put(bundle.getString("callback.transactionType"),Constants.OSTID_DEFAULT_TRANSACTIONTYPE);
                put(bundle.getString("callback.accountRef"),Constants.OSTID_DEFAULT_ACCOUNTREF);
                put(bundle.getString("callback.creditorName"),Constants.OSTID_DEFAULT_CREDITORNAME);
                put(bundle.getString("callback.creditorIBAN"),Constants.OSTID_DEFAULT_CREDITORIBAN);
        }};

        config.optionalAttributes().forEach(attr -> attrMap.putIfAbsent(attr,attr));
        attrMap.values().forEach(attrName -> attrValueMap.putIfAbsent(attrName,null));

        if( context.getCallbacks(NameCallback.class) != null &&
            context.getCallbacks(NameCallback.class).size() >= 7 ){
            context.getCallbacks(NameCallback.class)
                    .forEach(nameCallback -> {
                        if (attrMap.keySet().contains(nameCallback.getPrompt())) {
                            attrValueMap.put(attrMap.get(nameCallback.getPrompt()), nameCallback.getName());
                        }
                    });
        }

        boolean passKeyInclude = true;
        String password = "";
        if(config.passKeyRequired()){
            if(context.getCallbacks(PasswordCallback.class) != null && context.getCallbacks(PasswordCallback.class).size() > 0){
                PasswordCallback passwordCallback = context.getCallbacks(PasswordCallback.class).get(0);
                password = String.valueOf(passwordCallback.getPassword());
            }else{
                passKeyInclude = false;
            }
        }

        if(!CollectionsUtils.hasAnyNullValues(attrValueMap) && passKeyInclude) { //second time, with collected data
            for (Map.Entry<String, String> entry : attrValueMap.entrySet()) {
                sharedState.put(entry.getKey(), entry.getValue());
            }
            if(config.passKeyRequired()){
                transientState.put(Constants.OSTID_DEFAULT_PASSKEY,password);
            }
            logger.debug("OSTID_DEMO_TransactionCollector shared state: " + JSON.toJSONString(sharedState));
            logger.debug("OSTID_DEMO_TransactionCollector transient state: " + JSON.toJSONString(transientState));
            return goToNext()
                    .replaceSharedState(sharedState)
                    .replaceTransientState(transientState)
                    .build();
        }else{
            return Action.send(collectTransactionData(bundle)).build();
        }

    }

    private List<Callback> collectTransactionData(ResourceBundle bundle){
        List<Callback> callbackList = new ArrayList<>();

        NameCallback OSTID_DEFAULT_USERNAME = new NameCallback(bundle.getString("callback.username"));
        callbackList.add(OSTID_DEFAULT_USERNAME);
        if(config.passKeyRequired()) {
            PasswordCallback OSTID_DEFAULT_PASSKEY = new PasswordCallback(bundle.getString("callback.password"),false);
            callbackList.add(OSTID_DEFAULT_PASSKEY);
        }
        NameCallback OSTID_DEFAULT_ACCOUNTREF = new NameCallback(bundle.getString("callback.accountRef"));
        callbackList.add(OSTID_DEFAULT_ACCOUNTREF);

        NameCallback OSTID_DEFAULT_AMOUNT = new NameCallback(bundle.getString("callback.amount"));
        callbackList.add(OSTID_DEFAULT_AMOUNT);

        NameCallback OSTID_DEFAULT_CREDITORIBAN = new NameCallback(bundle.getString("callback.creditorIBAN"));
        callbackList.add(OSTID_DEFAULT_CREDITORIBAN);

        NameCallback OSTID_DEFAULT_CREDITORNAME = new NameCallback(bundle.getString("callback.creditorName"));
        callbackList.add(OSTID_DEFAULT_CREDITORNAME);

        NameCallback OSTID_DEFAULT_CURRENCY = new NameCallback(bundle.getString("callback.currency"));
        callbackList.add(OSTID_DEFAULT_CURRENCY);

        NameCallback OSTID_DEFAULT_TRANSACTIONTYPE = new NameCallback(bundle.getString("callback.transactionType"));
        callbackList.add(OSTID_DEFAULT_TRANSACTIONTYPE);

        for (String optionalAttribute : config.optionalAttributes()) {
            NameCallback NameCallback = new NameCallback(optionalAttribute);
            callbackList.add(NameCallback);
        }

        return callbackList;
    }

}
