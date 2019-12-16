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
package com.os.tid.forgerock.openam.backup;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.os.tid.forgerock.openam.config.Constants;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


/**
 * A node which collects CDDC information through script callback.
 *
 * <p>Places the result in the shared state as 'osstid_cddc_json', 'osstid_cddc_hash' and 'osstid_cddc_ip'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = OSTIDVisualCodeRenderNode.Config.class)
public class OSTIDVisualCodeRenderNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OSTIDVisualCodeRenderNode.Config config;

    /**
     * Configuration for the OS TID Cronto Node.
     */
    public interface Config {
        //todo, whether to push script (render <img> tag to front end)
        /**
         *
         * @return
         */
        //todo, should be a shared config, from register user node
        @Attribute(order = 100)
        default String userNameInSharedData() {
            return Constants.OSTID_DEFAULT_USERNAME;
        }
        /**
         * cronto or qr code
         */

        /**
         * size of image
         */

        /**
         * alt text for cronto image
         */

        /**
         * attribute name of cronto URL
         */



    }

    @Inject
    public OSTIDVisualCodeRenderNode(@Assisted OSTIDVisualCodeRenderNode.Config config){
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        logger.debug("OSTIDVisualCodeRenderNode started");


        //build hiddenValueCallback & scriptCallback
        HiddenValueCallback hiddenValueCDDCJson = new HiddenValueCallback(Constants.OSTID_CRONTO,"");
        String crontURL = "https://duoliang11061-mailin.dev.tid.onespan.cloud/visualcode/v1/render?format=CRONTO&message=30323b7573657231313037313b3131313b64756f6c69616e6731313037312d6d61696c696e3b5536674f6b6a31333b64756f6c69616e6731313037312d6d61696c696e&crontoImgSize=5";
        String crontoAlt = Constants.OSTID_DEFAULT_CRONTO_ALT;
        int crontoHeight = Constants.OSTID_DEFAULT_CRONTO_HEIGHT;

        String crontoScriptBase = "var crontoDiv = \"<div><img style='display:block;margin:auto;'  src='%s' alt='%s' height='%d' width='%d'></img><br/><p id='demo'></p></div>\";" +
                "document.getElementById('%s').parentElement.innerHTML += crontoDiv;";
        String crontoScript = String.format(crontoScriptBase,crontURL,crontoAlt,crontoHeight,crontoHeight,Constants.OSTID_CRONTO);
        ScriptTextOutputCallback crontoScriptCallback = new ScriptTextOutputCallback(crontoScript);

        JsonValue sharedState = context.sharedState;
        JsonValue hasRenderedJsonValue = sharedState.get("OS_TID_CROTON_HAS_RENDERED");
        if(hasRenderedJsonValue.isNotNull()){
            return Action.send(
                    ImmutableList.of(
                            hiddenValueCDDCJson,
                            crontoScriptCallback
                    )
            )
                    .build();
        }


        String displayScript =  "function display() {\n" +
                " var seconds = 101;\n" +
                "  function displayName() { // displayName() is the inner function, a closure\n" +
                "    // Update the count down every 1 second\n" +
                "    var x = setInterval(function() {\n" +
                "\n" +
                "\n" +
                "\n" +
                "      seconds = seconds - 1;\n" +
                "\n" +
                "      // Output the result in an element with id=\"demo\"\n" +
                "      document.getElementById(\"demo\").innerHTML = seconds + \"s \";\n" +
                "\n" +
                "      // If the count down is over, write some text \n" +
                "      if (seconds < 0) {\n" +
                "        document.getElementById(\"demo\").innerHTML = \"EXPIRED\";\n" +
                "      }\n" +
                "    }, 1000);\n" +
                "  }\n" +
                "  displayName();\n" +
                "}\n" +
                "display();\n"
                ;
        ScriptTextOutputCallback displayScriptCallback = new ScriptTextOutputCallback(displayScript);
        sharedState.put("OS_TID_CROTON_HAS_RENDERED",true);

        return Action.send(
                    ImmutableList.of(
                            hiddenValueCDDCJson,
                            crontoScriptCallback,
                            displayScriptCallback
                    )
                )
                .replaceSharedState(sharedState)
                .build();


    }



}
