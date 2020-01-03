package com.os.tid.forgerock.openam.utils;

import org.apache.commons.io.IOUtils;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ScriptUtils {
    private final static Logger logger = LoggerFactory.getLogger("amAuth");

    private  ScriptUtils(){}

    public static String getScriptFromFile(String scriptPath) throws NodeProcessException{
        String script;
        try (InputStreamReader inputStreamReader = new InputStreamReader(
                ScriptUtils.class.getResourceAsStream(scriptPath), StandardCharsets.UTF_8)) {
            script = IOUtils.toString(inputStreamReader);
        } catch (IOException e) {
            String errorMsg = "Error when loading script file: " + scriptPath + "; details: " + e.getMessage();
            logger.error(errorMsg);
            throw new NodeProcessException(errorMsg);
        }
        return script;
    }

}
