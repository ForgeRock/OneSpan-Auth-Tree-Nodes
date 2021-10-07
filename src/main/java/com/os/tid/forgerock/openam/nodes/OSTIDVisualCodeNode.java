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
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * This node reads the visual code message from the sharedState and renders it as a visual code, which allows the device integrated with the Mobile Security Suite SDKs to scan with.
 */
@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = OSTIDVisualCodeNode.Config.class,
                tags = {"OneSpan", "mfa"})
public class OSTIDVisualCodeNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDVisualCodeNode.Config config;
    private final OSTIDConfigurationsService serviceConfig;

    /**
     * Configuration for the OneSpan TID Visual Code Node.
     */
    public interface Config {
        /**
         *
         * @return
         */
        @Attribute(order = 100)
        default VisualCodeMessageOptions visualCodeMessageOption() {
            return VisualCodeMessageOptions.DemoMobileApp;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 200)
        default String customMessageInSharedState() {
            return "";
        }

        /**
         *
         * @return Hidden Value Callback Id contains the Visual Code URL
         */
        @Attribute(order = 300)
        default String visualCodeHiddenValueId() {
            return Constants.OSTID_CRONTO;
        }

        /**
         * Whether to push script (render <img> tag to front end)
         * If false, only put Visual Code URL as a hiddenValueCallback
         *
         * @return
         */
        @Attribute(order = 400)
        default boolean renderVisualCodeInCallback() {
            return true;
        }

        /**
         *
         * @return
         */
        @Attribute(order = 500)
        default String domIdRenderVisualCode() {
            return "dialog";
        }

        /**
         * cronto or qr code, enum
         */
        @Attribute(order = 600)
        default VisualCodeType visualCodeType() {
            return VisualCodeType.Cronto;
        }

        /**
         * size of image
         */
        @Attribute(order = 700)
        default int sizeOfVisualCode() {
            return Constants.OSTID_DEFAULT_CRONTO_HEIGHT;
        }

        /**
         * alt text for cronto image
         */
        @Attribute(order = 800)
        default String altTextOfVisualCode() {
            return Constants.OSTID_DEFAULT_CRONTO_ALT;
        }

        /**
         * please scan
         */
        @Attribute(order = 900)
        default String textForPleaseScan() {
            return "Please Scan the Visual Code within:";
        }

        /**
         * please scan
         */
        @Attribute(order = 1000)
        default String cssForPleaseScan() {
            return "";
        }

        /**
         * has been expired
         */
        @Attribute(order = 1100)
        default String textForExpired() {
            return "Your Activation Code has been expired!";
        }

        /**
         * has been expired
         */
        @Attribute(order = 1200)
        default String cssForExpired() {
            return "";
        }
    }

    @Inject
    public OSTIDVisualCodeNode(@Assisted OSTIDVisualCodeNode.Config config, @Assisted Realm realm, AnnotatedServiceRegistry serviceRegistry) throws NodeProcessException {
        this.config = config;
        try {
            this.serviceConfig = serviceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm).get();
        } catch (SSOException | SMSException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OSTIDVisualCodeNode started");
        JsonValue sharedState = context.sharedState;
        String tenantName = serviceConfig.tenantNameToLowerCase();
        String environment = serviceConfig.environment().name();

        JsonValue crontoMsgJsonValue = config.visualCodeMessageOption() == VisualCodeMessageOptions.CustomCrontoMessage ? sharedState.get(config.customMessageInSharedState()) : sharedState.get(Constants.OSTID_CRONTO_MSG);
        boolean hasConsumed = false;
        if(context.getCallbacks(HiddenValueCallback.class) != null && context.getCallbacks(HiddenValueCallback.class).size() >= 1){
            for (HiddenValueCallback hiddenValueCallback : context.getCallbacks(HiddenValueCallback.class)) {
                if (Constants.OSTID_CRONTO_HAS_RENDERED.contains(hiddenValueCallback.getId())) {
                    hasConsumed = hiddenValueCallback.getValue().equalsIgnoreCase("true");
                }
            }
        }

        //1. throw exception, if user input is not intact
        if(!crontoMsgJsonValue.isString() ){
            logger.debug("OSTIDVisualCodeNode crontoMsgJsonValue is null: " + crontoMsgJsonValue.isNull());
            throw new NodeProcessException("Can't find Cronto Message in shared state!");
        }
        //2. go to next
        else if(hasConsumed) {
            sharedState.remove(config.visualCodeHiddenValueId());
            return goToNext().replaceSharedState(sharedState).build();
        }
        //3. send to page
        else {
            List<Callback> returnCallback = new ArrayList<>();

            //return visual code URL as hiddenValueCallback
            String crontURL = "";
            if (sharedState.get(config.visualCodeHiddenValueId()).isNull()) {
                crontURL = StringUtils.getAPIEndpoint(tenantName,environment) + String.format(Constants.OSTID_API_ADAPTIVE_CRTONTO_RENDER,
                        config.visualCodeType().name().toUpperCase(),
                        crontoMsgJsonValue.asString());
                sharedState.put(config.visualCodeHiddenValueId(), crontURL);
            } else {
                crontURL = sharedState.get(config.visualCodeHiddenValueId()).asString();
            }
            HiddenValueCallback hiddenValueCDDCJson = new HiddenValueCallback(config.visualCodeHiddenValueId(), crontURL);
            HiddenValueCallback expiryDateCallback = new HiddenValueCallback(Constants.OSTID_EVENT_EXPIRY_DATE, getExpiryString(sharedState));
            HiddenValueCallback hasConsumedCallback = new HiddenValueCallback(Constants.OSTID_CRONTO_HAS_RENDERED, Constants.OSTID_CRONTO_HAS_RENDERED);
            returnCallback.add(hiddenValueCDDCJson);
            returnCallback.add(expiryDateCallback);
            returnCallback.add(hasConsumedCallback);

            //return Visual Code Script if required
            if (config.renderVisualCodeInCallback() ) {
                if(sharedState.get(Constants.OSTID_CRONTO_PUSH_JS).isNull()) {
                    addCrontoScript(returnCallback);
                    sharedState.put(Constants.OSTID_CRONTO_PUSH_JS, true);
                }
                addStartScript(sharedState, crontURL, returnCallback);
            }

            return Action.send(returnCallback)
                    .replaceSharedState(sharedState)
                    .build();
        }
    }

    private void addCrontoScript(List<Callback> returnCallback ){
        String displayScript =
                        "function CDDC_display(isStart) {" +
                        " var CDDC_timer;" +
                        " function start(countDownDate,countdownText,expiryText,imageSrc,imageAlt,imageHeight,imageLocDomID,countdownCSS,expiryCSS) { " +
                        "    addCrontoUI(imageSrc,imageAlt,imageHeight,imageLocDomID);"+
                        "    this.CDDC_timer = setInterval(function() {" +
                        "       var now = new Date().getTime();" +
                        "       var distance = countDownDate - now;      " +
                        "       var seconds = Math.floor((distance / 1000) % 3600);" +
                        "       document.getElementById('ostid_cronto_countdown').innerHTML = '<p style=\"' + countdownCSS + '\">' + countdownText + \" \" + seconds + ' s</p>';" +
                        "       if (seconds < 0) {" +
                        "            document.getElementById('ostid_cronto_countdown').innerHTML = '<p style=\"' + expiryCSS + '\">' + expiryText + '</p>';" +
                        "            clearTimeout(this.CDDC_timer); " +
                        "       }" +
                        "    }, 1000);" +
                        "  }" +
                        " function stop() { " +
                        "      clearTimeout(this.CDDC_timer); " +
                        "      removeCrontoUI();  " +
                        "  }" +
                        " function addCrontoUI(imageSrc,imageAlt,imageHeight,imageLocDomID) { " +
                        "       var crontoDiv = \"<div id='ostid_cronto'>" +
                        "                           <img style='display:block;margin:auto;'  src='\"+imageSrc+\"' alt='\"+imageAlt+\"' height='\"+imageHeight+\"' width='\"+imageHeight+\"'></img><br/>" +
                        "                           <p id='ostid_cronto_countdown' style='text-align:center'></p>" +
                        "                         </div>\";" +
                        "       if(document.getElementById('ostid_cronto')){" +
                        "           document.getElementById('ostid_cronto').innerHTML = crontoDiv;" +
                        "       }else{" +
                        "           var helper = document.createElement('div');" +
                        "           helper.innerHTML = crontoDiv;"+
                        "           insertAfter(document.getElementById(imageLocDomID),helper);}"+



//                        "           document.getElementById(imageLocDomID).innerHTML += crontoDiv;}" +
                        "       var style = document.createElement('style');" +
                        "       style.type = 'text/css';" +
                        "       style.id = 'ostid_cronto_style';" +
                        "       style.innerHTML = '.spinner { display: none!important;} .panel-default{display:none!important;}';" +
                        "       document.getElementsByTagName('head')[0].appendChild(style);" +
                        "  }" +
                        " function removeCrontoUI() { " +
                        "      document.getElementById('ostid_cronto').remove();  " +
                        "      document.getElementById('ostid_cronto_style').remove();  " +
                        "  }" +

                        " function insertAfter(referenceNode, newNode) {" +
                        "   if(referenceNode.parentNode){"+
                        "         referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);" +
                        "   }else{" +
                        "        referenceNode.innerHtml += newNode.innerHtml;" +
                        "   }"+
                        " }" +
                        " return isStart == true ? start : stop;" +
                        "}" +
                        "var CDDC_start = CDDC_display(true);" +
                        "var CDDC_stop = CDDC_display(false);" ;

        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(displayScript);
        returnCallback.add(displayScriptCallback);
    }

    private void addStartScript(JsonValue sharedState, String crontURL, List<Callback> returnCallback ){
        String expiryDateInMilli = getExpiryString(sharedState);

        String displayScriptBase =
                //extra code to inject hidden callback value
                "document.getElementById('loginButton_0').style.display = 'none';"+
                "if (CDDC_start && typeof CDDC_start === 'function') { " +
                "   CDDC_start(%1$s,'%2$s','%3$s','%4$s','%5$s',%6$d,'%7$s','%8$s','%9$s');" +
                "}" +
                "document.getElementById('%10$s').value = 'true';" +
                "document.getElementById('loginButton_0').click();";

        // function start(seconds,countdownText,expiryText,imageSrc,imageAlt,imageHeight,imageLocDomID)
        String displayScript = String.format(displayScriptBase,
                expiryDateInMilli,                  //param1
                config.textForPleaseScan(),         //param2
                config.textForExpired(),            //param3
                crontURL,                           //param4
                config.altTextOfVisualCode(),       //param5
                config.sizeOfVisualCode(),          //param6
                config.domIdRenderVisualCode(),     //param7
                config.cssForPleaseScan(),          //param8
                config.cssForExpired(),             //param9
                Constants.OSTID_CRONTO_HAS_RENDERED //param10
        );

        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(displayScript);
        returnCallback.add(displayScriptCallback);
    }

    public enum VisualCodeType {
        Cronto,QR
    }

    public enum VisualCodeMessageOptions {
        DemoMobileApp, CustomCrontoMessage
    }

    private String getExpiryString(JsonValue sharedState){
        JsonValue activationTokenExpiryDateJsonValue = sharedState.get(Constants.OSTID_EVENT_EXPIRY_DATE);
        String expiryDateInMilli =  activationTokenExpiryDateJsonValue.isNull() ? DateUtils.getMilliStringAfterCertainSecs(Constants.OSTID_DEFAULT_EVENT_EXPIRY) :
                activationTokenExpiryDateJsonValue.asString();
        return expiryDateInMilli;
    }
}
