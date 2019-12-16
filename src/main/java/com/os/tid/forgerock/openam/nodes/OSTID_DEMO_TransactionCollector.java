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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.*;


/**
 * A node which collects CDDC information through script callback.
 *
 * <p>Places the result in the shared state as 'osstid_cddc_json', 'osstid_cddc_hash' and 'osstid_cddc_ip'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = OSTID_DEMO_TransactionCollector.Config.class)
public class OSTID_DEMO_TransactionCollector extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTID_DEMO_TransactionCollector.Config config;
    /**
     * Configuration for the OS TID DEMO Transaction Collector.
     */
    public interface Config {
        /**
         *
         * @return
         */
        @Attribute(order = 100)
        default boolean passKeyRequired() {
            return false;
        }

        /**
         * Configurable attributes in request JSON payload
         *
         * @return
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

        //try finding CDDC valus from hiddenValueCallback
        Map<String, String> attrValueMap = new HashMap<>();
        Set<String> attrNameSet = new HashSet<>(Arrays.asList(
                Constants.OSTID_DEFAULT_USERNAME,
                Constants.OSTID_DEFAULT_TRANSACTIONTYPE,
                Constants.OSTID_DEFAULT_ACCOUNTREF,
                Constants.OSTID_DEFAULT_AMOUNT,
                Constants.OSTID_DEFAULT_CREDITORIBAN,
                Constants.OSTID_DEFAULT_CREDITORNAME,
                Constants.OSTID_DEFAULT_CURRENCY,
                Constants.OSTID_DEFAULT_DEBTORNAME
        ));
        attrNameSet.addAll(config.optionalAttributes());
        attrNameSet.forEach(attrName -> attrValueMap.putIfAbsent(attrName,null));

        if( context.getCallbacks(NameCallback.class) != null &&
            context.getCallbacks(NameCallback.class).size() >= 8 ){
            context.getCallbacks(NameCallback.class)
                    .forEach(nameCallback -> {
                        if (attrNameSet.contains(nameCallback.getPrompt())) {
                            attrValueMap.put(nameCallback.getPrompt(), nameCallback.getName());
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
            return Action.send(collectTransactionData()).build();
        }

    }

    private List<Callback> collectTransactionData(){
        List<Callback> callbackList = new ArrayList<>();
        NameCallback OSTID_DEFAULT_USERNAME = new NameCallback(Constants.OSTID_DEFAULT_USERNAME);
        callbackList.add(OSTID_DEFAULT_USERNAME);
        if(config.passKeyRequired()) {
            PasswordCallback OSTID_DEFAULT_PASSKEY = new PasswordCallback(Constants.OSTID_DEFAULT_PASSKEY,false);
            callbackList.add(OSTID_DEFAULT_PASSKEY);
        }
        NameCallback OSTID_DEFAULT_ACCOUNTREF = new NameCallback(Constants.OSTID_DEFAULT_ACCOUNTREF);
        callbackList.add(OSTID_DEFAULT_ACCOUNTREF);

        NameCallback OSTID_DEFAULT_AMOUNT = new NameCallback(Constants.OSTID_DEFAULT_AMOUNT);
        callbackList.add(OSTID_DEFAULT_AMOUNT);

        NameCallback OSTID_DEFAULT_CREDITORIBAN = new NameCallback(Constants.OSTID_DEFAULT_CREDITORIBAN);
        callbackList.add(OSTID_DEFAULT_CREDITORIBAN);

        NameCallback OSTID_DEFAULT_CREDITORNAME = new NameCallback(Constants.OSTID_DEFAULT_CREDITORNAME);
        callbackList.add(OSTID_DEFAULT_CREDITORNAME);

        NameCallback OSTID_DEFAULT_CURRENCY = new NameCallback(Constants.OSTID_DEFAULT_CURRENCY);
        callbackList.add(OSTID_DEFAULT_CURRENCY);

        NameCallback OSTID_DEFAULT_TRANSACTIONTYPE = new NameCallback(Constants.OSTID_DEFAULT_TRANSACTIONTYPE);
        callbackList.add(OSTID_DEFAULT_TRANSACTIONTYPE);

        NameCallback OSTID_DEFAULT_DEBTORNAME = new NameCallback(Constants.OSTID_DEFAULT_DEBTORNAME);
        callbackList.add(OSTID_DEFAULT_DEBTORNAME);

        for (String optionalAttribute : config.optionalAttributes()) {
            NameCallback NameCallback = new NameCallback(optionalAttribute);
            callbackList.add(NameCallback);
        }

        return callbackList;
    }

}
