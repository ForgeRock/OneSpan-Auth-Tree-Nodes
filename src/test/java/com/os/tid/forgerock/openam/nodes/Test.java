package com.os.tid.forgerock.openam.nodes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.os.tid.forgerock.openam.utils.ScriptUtils;
import com.os.tid.forgerock.openam.utils.StringUtils;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import org.apache.commons.io.IOUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

import javax.security.auth.callback.NameCallback;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

public class Test {

    public static void main(String[] args) throws Exception {
        System.out.println(UUID.randomUUID().toString());

        System.out.println(
                "function CDDC_display(isStart) {" +
                        " var CDDC_timer;" +
                        " function start(seconds,countdownText,expiryText,imageSrc,imageAlt,imageHeight,imageLocDomID) { " +
                        "      addCrontoUI(imageSrc,imageAlt,imageHeight,imageLocDomID);"+
                        "      this.CDDC_timer = setInterval(function() {" +
                        "      document.getElementById('ostid_cronto_countdown').innerHTML = countdownText + seconds + ' s ';" +
                        "      if (seconds < 0) {" +
                        "        document.getElementById('ostid_cronto_countdown').innerHTML = expiryText;" +
                        "      }" +
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
                        "       document.getElementById(imageLocDomID).innerHTML += crontoDiv;" +
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
                        " return isStart == true ? start : stop;" +
                        "}" +
                        "var CDDC_start = CDDC_display(true);" +
                        "var CDDC_stop = CDDC_display(false);" );
        //=========
//        String displayScriptBase =
//                "function display() {\n" +
//                        " var countDownDate = %1$s;\n"+
//                        " function displayName() { // displayName() is the inner function, a closure\n" +
//                        "      // Update the count down every 1 second\n" +
//                        "   var x = setInterval(function() {\n" +
//                        "      var now = new Date().getTime();\n"+
//                        "      var distance = countDownDate - now;"+
//                        "      var seconds = Math.floor((distance %% (1000 * 60)) / 1000);\n" +
//                        "      // Output the result in an element with id=\"ostid_cronto_countdown\"\n" +
//                        "      document.getElementById(\"ostid_cronto_countdown\").innerHTML = \"%2$s\"+seconds + \" s \";\n" +
//                        "      // If the count down is over, write some text \n" +
//                        "      if (seconds < 0) {\n" +
//                        "        document.getElementById(\"ostid_cronto_countdown\").innerHTML = \"%3$s\";\n" +
//                        "      }\n" +
//                        "    }, 1000);\n" +
//                        "  }\n" +
//                        " displayName();\n" +
//                        "}\n" +
//                        "display();\n";
//        System.out.println("displayScriptBase = " + displayScriptBase);
//        String displayScript = String.format(displayScriptBase,"1574368367985", "Please Scan the Visual Code within: ","Your Activation Code has been expired!");
//        System.out.println("displayScript = " + displayScript);
//
//        //=========
//        System.out.println("DateUtils.getMilliStringAfterCertainSecs(300) = " + DateUtils.getMilliStringAfterCertainSecs(300));
//
//
//        //==========
//        //try finding CDDC valus from hiddenValueCallback
//        Map<String, String> attrValueMap = new HashMap<>();
//        ImmutableSet<String> attrNameSet = ImmutableSet.of(Constants.OSTID_CDDC_HASH, Constants.OSTID_CDDC_JSON);
//        attrNameSet.forEach(attrName -> attrValueMap.putIfAbsent(attrName, null));
//
//        ImmutableList.of(new HiddenValueCallback(Constants.OSTID_CDDC_HASH, "Constants.OSTID_CDDC_HASH value"),
//                new HiddenValueCallback(Constants.OSTID_CDDC_JSON, "Constants.OSTID_CDDC_JSON value"))
//                .forEach(hiddenValueCallback -> {
//                    if (attrNameSet.contains(hiddenValueCallback.getId())) {
//                        attrValueMap.put(hiddenValueCallback.getId(), hiddenValueCallback.getValue());
//                    }
//                });
//
//        System.out.println("attrValueMap = " + attrValueMap);
//
//        //==========
//        String crontURL = Constants.OSTID_API_URL + String.format(Constants.OSTID_API_CRTONTO_RENDER,"CRONTO","35343838653134362d386538372d343534342d383964392d663333336235656130303361");
//        String crontoAlt = Constants.OSTID_DEFAULT_CRONTO_ALT;
//        int crontoHeight = Constants.OSTID_DEFAULT_CRONTO_HEIGHT;
//
//        String crontoScriptBase = "var crontoDiv = \"<div><img style='display:block;margin:auto;'  src='%s' alt='%s' height='%d' width='%d'></img></div>\";" +
//                "document.getElementsByClassName('container')[0].innerHTML += crontoDiv;";
//        String crontoScript = String.format(crontoScriptBase,crontURL,crontoAlt,crontoHeight,crontoHeight);
//        System.out.println("crontoScript = " + crontoScript);
//
//        //=========
//        String this_is_my_custom_hex = StringUtils.toHex("this is my custom hex");
//        System.out.println("this_is_my_custom_hex = " + this_is_my_custom_hex);
//
//
//        //===========
//        String format = String.format(Constants.OSTID_JSON_USER_REGISTER, "login", "clientip","fingerprinthash","fingerprintraw","sessionIdentifier","passkey");
//
//        System.out.println("format = " + format);
//
//        //============
//        ImmutableMap<String, List<Class<? extends Node>>> of = ImmutableMap.of(
//                "1.0.0", Arrays.asList(
//                        OSTIDCDDCNode.class,
//                        OSTIDUserRegisterNode.class
//                ));
//        System.out.println("of = " + of);
//
//        //=============
//        String helloScript = ScriptUtils.getScriptFromFile("/js/hello-world.js");
//        System.out.println("helloScript = " + helloScript);
//
//        //===============
//        InputStream resourceStream = ScriptUtils.class.getResourceAsStream("/js/hello-world.js");
//        String script = "";
//        try {
//            script = IOUtils.toString(resourceStream, "UTF-8");
//        } catch (IOException e) {
//        }
//        System.out.println("script = " + script);
//
//
//        //================
//        String BUNDLE = "org/os/tid/forgerock/openam/nodes/OSTIDCDDCAuthNode";
//
//        JsonValue sharedState = json(object(field("initial", "initial")));
//        NameCallback callback = new NameCallback("prompt");
//        callback.setName("bob");
//        TreeContext context = new TreeContext(sharedState, new ExternalRequestContext.Builder().locales(new PreferredLocales()).build(), Collections.singletonList(callback));
//        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, OSTIDUserRegisterNode.class.getClassLoader());
//
//        String bundleString = bundle.getString("nodeDescription");
//        System.out.println("bundleString = " + bundleString);

    }
}
