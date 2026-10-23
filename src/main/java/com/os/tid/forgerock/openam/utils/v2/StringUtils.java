package com.os.tid.forgerock.openam.utils.v2;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.utils.RedactUtils;

public class StringUtils {
    private final static Logger logger = LoggerFactory.getLogger("amAuth");

    private StringUtils() {
    }

    public static boolean isEmpty(String string){
        return string == null || "".equals(string);
    }

    /**
     * Escapes a value so it can be safely interpolated into a JSON string literal
     * built with String.format (OPENAM-29243 credential injection).
     */
    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return StringEscapeUtils.escapeJava(value);
    }

    /**
     * Percent-encodes a value so it can be safely interpolated into a URL path
     * segment or query parameter (OPENAM-29243 credential injection).
     */
    public static String encodeUrlValue(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            // Unreserved characters (RFC 3986): ALPHA / DIGIT / "-" / "." / "_" / "~"
            if ((b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9')
                    || b == '-' || b == '.' || b == '_' || b == '~') {
                encoded.append((char) b);
            } else {
                encoded.append('%').append(String.format("%02X", b));
            }
        }
        return encoded.toString();
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
    public static String getAPIEndpoint(String ostid_tenant_name, String ostid_environment, String ostid_custom_url){
    	String ApiEndpoint = "";
    	if(ostid_environment.equalsIgnoreCase(Constants.OSTID_ENV_MAP.get(OSConfigurationsService.EnvOptions.CUSTOMIZED))) {
    		ApiEndpoint = ostid_custom_url;
    	}else {
    		ApiEndpoint = String.format("https://%1$s.%2$s.tid.onespan.cloud",ostid_tenant_name,ostid_environment);
    	}
        return ApiEndpoint;
    }

    /**
     * The getErrorMsg* helpers redact the request payload (OPENAM-29243 sensitive logging): payloads carry user
     * credentials (password/OTP/signature) and these messages reach shared state and the end-user error display,
     * not just the logs.
     */
    public static String getErrorMsgNoRetCodeWithoutValidation(String message, String log_correlation_id, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %3$s;<br />Log Correction ID: %2$s;",
                message,log_correlation_id,RedactUtils.redactJson(requestJSON)
        );
    }

    public static String getErrorMsgNoRetCodeWithValidation(String message, String log_correlation_id, String validationMsg, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %4$s;<br />Validation Message: %3$s;<br />Log Correction ID: %2$s;",
                message,log_correlation_id,validationMsg, RedactUtils.redactJson(requestJSON)
        );
    }


    public static String getErrorMsgWithoutValidation(String message, String retCode, String log_correlation_id, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %4$s;<br />RetCode: %2$s;<br />Log Correction ID: %3$s;",
                message,retCode,log_correlation_id,RedactUtils.redactJson(requestJSON)
                );
    }

    public static String getErrorMsgWithValidation(String message, String retCode, String log_correlation_id, String validationMsg, String requestJSON){
        return String.format("Error Message: %1$s;<br />Request Payload: %5$s;<br />Validation Message: %4$s;<br />RetCode: %2$s;<br />Log Correction ID: %3$s;",
                message,retCode,log_correlation_id,validationMsg,RedactUtils.redactJson(requestJSON)
        );
    }

    public static String getErrorMsgWithValidation2(String message, String error, String log_correlation_id, String validationMsg, String requestJSON){
        return String.format("Error: %2$s;<br />Error Message: %1$s;<br />Request Payload: %5$s;<br />Validation Message: %4$s;<br />Log Correction ID: %3$s;",
                message,error,log_correlation_id,validationMsg,RedactUtils.redactJson(requestJSON)
        );
    }

    public static String getErrorMsgWithoutValidation2(String message, String error, String log_correlation_id, String requestJSON){
        return String.format("Error: %2$s;<br />Error Message: %1$s;<br />Request Payload: %4$s;<br />Log Correction ID: %3$s;",
                message,error,log_correlation_id,RedactUtils.redactJson(requestJSON)
        );
    }

}
