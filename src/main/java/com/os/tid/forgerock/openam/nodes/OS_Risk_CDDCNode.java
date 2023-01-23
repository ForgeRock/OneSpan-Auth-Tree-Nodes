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

import com.google.common.collect.ImmutableSet;
import com.google.inject.assistedinject.Assisted;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OS_Auth_UserLoginNode.UserLoginOutcome;
import com.os.tid.forgerock.openam.utils.CollectionsUtils;
import com.os.tid.forgerock.openam.utils.ScriptUtils;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A node which collects CDDC information through script callback.
 * Places the result in the shared state as 'osstid_cddc_json', 'osstid_cddc_hash' and 'osstid_cddc_ip'.
 */
@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = OS_Risk_CDDCNode.Config.class,
                tags = {"OneSpan", "multi-factor authentication", "marketplace", "trustnetwork"})
public class OS_Risk_CDDCNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OS_Risk_CDDCNode.Config config;
    private static final String loggerPrefix = "[OneSpan Risk CDDC][Marketplace] ";

    /**
     * Configuration for the OS Risk CDDC Collector Node.
     */
    public interface Config {
        /**
         * If false, allows user to make POST API to invoke this node
         */
        @Attribute(order = 100)
        default boolean pushCDDCJsAsCallback() {
            return true;
        }

        /**
         * The hidden callback ID for the CDDC Json collector
         */
        @Attribute(order = 200)
        default String CDDCJsonHiddenValueId() {
            return Constants.OSTID_CDDC_JSON;
        }

        /**
         * The hidden callback ID for the CDDC Hash collector
         */
        @Attribute(order = 300)
        default String CDDCHashHiddenValueId() {
            return Constants.OSTID_CDDC_HASH;
        }
    }

    @Inject
    public OS_Risk_CDDCNode(@Assisted Config config){
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
    	try {
	        logger.debug(loggerPrefix + "OS_Risk_CDDCNode started");
	        JsonValue sharedState = context.sharedState.copy();
	
	        Map<String, String> attrValueMap = new HashMap<>();
	        ImmutableSet<String> attrNameSet = ImmutableSet.of(getCDDCJsonInHiddenValue(), getCDDCHashInHiddenValue());
	        attrNameSet.forEach(attrName -> attrValueMap.putIfAbsent(attrName,null));
	
	        if(context.getCallbacks(HiddenValueCallback.class) != null && context.getCallbacks(HiddenValueCallback.class).size() >= 2){
	           context.getCallbacks(HiddenValueCallback.class)
	                    .forEach(hiddenValueCallback -> {
	                        if (attrNameSet.contains(hiddenValueCallback.getId())) {
	                            attrValueMap.put(hiddenValueCallback.getId(), hiddenValueCallback.getValue());
	                        }
	                    });
	        }
	
	        if(!CollectionsUtils.hasAnyNullValues(attrValueMap)) {                  //1. the second time, with intact data
	            logger.debug(loggerPrefix + "OS_Risk_CDDCNode with CDDC JSON and Hash!");
	
	            String CDDCJson = attrValueMap.get(getCDDCJsonInHiddenValue());
	            String CDDCHash = attrValueMap.get(getCDDCHashInHiddenValue());
	            String CDDCIp = context.request.clientIp;
	            if("0:0:0:0:0:0:0:1".equals(CDDCIp)){
	                CDDCIp = "127.0.0.1";
	            }
	
	            sharedState.put(Constants.OSTID_CDDC_JSON,CDDCJson);
	            sharedState.put(Constants.OSTID_CDDC_HASH,CDDCHash);
	            sharedState.put(Constants.OSTID_CDDC_IP,CDDCIp);
	
	            return goToNext()
	                    .replaceSharedState(sharedState)
	                    .build();
	        }else {                                                                 //2. the first time, without collected data
	            logger.debug(loggerPrefix + "OS_Risk_CDDCNode without CDDC JSON and Hash!");
	
	            List<Callback> returnCallback = new ArrayList<>();
	            HiddenValueCallback hiddenValueCDDCJson = new HiddenValueCallback(Constants.OSTID_CDDC_JSON,"");
	            HiddenValueCallback hiddenValueCDDCHash = new HiddenValueCallback(Constants.OSTID_CDDC_HASH,"");
	            returnCallback.add(hiddenValueCDDCJson);
	            returnCallback.add(hiddenValueCDDCHash);
	            if(config.pushCDDCJsAsCallback()){
	                //only push CDDC JS once
	                JsonValue hasPushedJSJsonValue = sharedState.get(Constants.OSTID_CDDC_HAS_PUSHED_JS);
	                if(hasPushedJSJsonValue.isNull()) {
	                    String jqueryScript = ScriptUtils.getScriptFromFile("/js/jquery-3.5.1.min.js");
	                    ScriptTextOutputCallback jqueryScriptCallback = new ScriptTextOutputCallback(jqueryScript);
	
	                    String JsonScript = ScriptUtils.getScriptFromFile("/js/Json2.js");
	                    ScriptTextOutputCallback JsonScriptScriptCallback = new ScriptTextOutputCallback(JsonScript);
	
	                    String CDDCScript = ScriptUtils.getScriptFromFile("/js/Vasco.IdKey.RM.CDDC.min.js");
	                    ScriptTextOutputCallback CDDCScriptCallback = new ScriptTextOutputCallback(CDDCScript);
	
	                    returnCallback.add(jqueryScriptCallback);
	                    returnCallback.add(JsonScriptScriptCallback);
	                    returnCallback.add(CDDCScriptCallback);
	                    sharedState.put(Constants.OSTID_CDDC_HAS_PUSHED_JS, true);
	                }
	
	                String customCDDCScriptBase =
	                        "console.log(typeof loginHelpers)\n" +
	                        "if(typeof loginHelpers !== 'undefined'){\n" +
	                        "   loginHelpers.setHiddenCallback('%1$s', $.Vasco.getJSON(true));\n" +
	                        "   loginHelpers.setHiddenCallback('%2$s', $.Vasco.getHASH(true));}\n" +
	                        "else{\n" +
	                        "   document.getElementById('%1$s').value = $.Vasco.getJSON(true);\n" +
	                        "   document.getElementById('%2$s').value = $.Vasco.getHASH(true);}";
	                String customCDDCScript = String.format(customCDDCScriptBase, Constants.OSTID_CDDC_JSON,Constants.OSTID_CDDC_HASH);
	                ScriptTextOutputCallback customCDDCScriptCallback = new ScriptTextOutputCallback(customCDDCScript);
	                returnCallback.add(customCDDCScriptCallback);
	
	            }
	            return Action.send(returnCallback)
	                    .replaceSharedState(sharedState)
	                    .build();
	        }
        
    	}catch (Exception ex) {
			logger.error(loggerPrefix + "Exception occurred: " + ex.getMessage());
			logger.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
			ex.printStackTrace();
			context.getStateFor(this).putShared("OS_Risk_CDDCNode Exception", new Date() + ": " + ex.getMessage())
									 .putShared(Constants.OSTID_ERROR_MESSAGE, "OneSpan Risk CDDC: " + ex.getMessage());
			throw new NodeProcessException(ex.getMessage());
	    }
    }

    private String getCDDCJsonInHiddenValue(){return config.pushCDDCJsAsCallback() ? Constants.OSTID_CDDC_JSON : config.CDDCJsonHiddenValueId();}
    private String getCDDCHashInHiddenValue(){return config.pushCDDCJsAsCallback() ? Constants.OSTID_CDDC_HASH : config.CDDCHashHiddenValueId();}

}
