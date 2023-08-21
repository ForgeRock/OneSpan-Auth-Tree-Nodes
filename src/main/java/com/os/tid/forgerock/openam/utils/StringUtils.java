package com.os.tid.forgerock.openam.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;

public class StringUtils {
    private final static Logger logger = LoggerFactory.getLogger("amAuth");

    private StringUtils() {
    }

    public static boolean isEmpty(String string){
        return string == null || "".equals(string);
    }

    public static String stringToHex(String string) {
        return String.format("%040x", new BigInteger(1, string.getBytes(StandardCharsets.UTF_8)));
    }

    public static String stringToHex2(String string) {
        return Hex.encodeHexString(string.getBytes());
    }

    public static String hexToString(String hex){
        try {
            return new String(Hex.decodeHex(hex.toCharArray()));
        } catch (DecoderException e) {
            logger.error("StringUtils exception: " + e.getMessage());
            return "";
        }
    }

    //"https://duoliang11071-mailin.sdb.tid.onespan.cloud"
    public static String getAPIEndpoint(String ostid_tenant_name, String ostid_environment){
    	String ApiEndpoint = "";
    	if(ostid_environment.equalsIgnoreCase(Constants.OSTID_ENV_MAP.get(OSConfigurationsService.EnvOptions.CUSTOMIZED))) {
    		ApiEndpoint = String.format("https://%1$s",ostid_tenant_name);
    	}else {
    		ApiEndpoint = String.format("https://%1$s.%2$s.tid.onespan.cloud",ostid_tenant_name,ostid_environment);
    	}
        return ApiEndpoint;
    }

    public static String getErrorMsgNoRetCodeWithoutValidation(String message, String log_correlation_id, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %3$s;<br />Log Correction ID: %2$s;",
                message,log_correlation_id,requestJSON
        );
    }

    public static String getErrorMsgNoRetCodeWithValidation(String message, String log_correlation_id, String validationMsg, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %4$s;<br />Validation Message: %3$s;<br />Log Correction ID: %2$s;",
                message,log_correlation_id,validationMsg, requestJSON
        );
    }


    public static String getErrorMsgWithoutValidation(String message, String retCode, String log_correlation_id, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %4$s;<br />RetCode: %2$s;<br />Log Correction ID: %3$s;",
                message,retCode,log_correlation_id,requestJSON
                );
    }

    public static String getErrorMsgWithValidation(String message, String retCode, String log_correlation_id, String validationMsg, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %5$s;<br />Validation Message: %4$s;<br />RetCode: %2$s;<br />Log Correction ID: %3$s;",
                message,retCode,log_correlation_id,validationMsg,requestJSON
        );
    }

    public static String getErrorMsgWithValidation2(String message, String error, String log_correlation_id, String validationMsg, String requestJSON){
        return String.format("Error: %2$s;<br />Error Message: %1$s;<br />Request Payload: %5$s;<br />Validation Message: %4$s;<br />Log Correction ID: %3$s;",
                message,error,log_correlation_id,validationMsg,requestJSON
        );
    }

    public static String getErrorMsgWithoutValidation2(String message, String error, String log_correlation_id, String requestJSON){
        return String.format("Error: %2$s;<br />Error Message: %1$s;<br />Request Payload: %4$s;<br />Log Correction ID: %3$s;",
                message,error,log_correlation_id,requestJSON
        );
    }

}
