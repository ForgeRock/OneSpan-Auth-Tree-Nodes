package com.os.tid.forgerock.openam.utils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.os.tid.forgerock.openam.utils.json.JSON;
import com.os.tid.forgerock.openam.utils.json.JSONArray;
import com.os.tid.forgerock.openam.utils.json.JSONObject;
import com.google.common.collect.ImmutableSet;

/**
 * Redacts sensitive values from JSON text before it is written to logs (OPENAM-29243).
 */
public final class RedactUtils {

    public static final String REDACTED = "***REDACTED***";

    /** Lower-cased fragments of JSON keys whose values must never be logged. */
    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = ImmutableSet.of(
            "password", "passkey", "otp", "signature", "authenticationresponse", "devicecode");

    private RedactUtils() {
    }

    /**
     * Redacts sensitive values from a JSON string. If the text is not valid JSON on its own,
     * the first {...} block is redacted (covers "METHOD url : {payload}" error messages);
     * anything else is replaced by a placeholder so unredactable text never reaches the logs.
     */
    public static String redactJson(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        try {
            return JSON.toJSONString(redactValue(JSON.parse(text)));
        } catch (Exception notWholeJson) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return text.substring(0, start)
                            + JSON.toJSONString(redactValue(JSON.parse(text.substring(start, end + 1))))
                            + text.substring(end + 1);
                } catch (Exception notEmbeddableJson) {
                    // fall through to the placeholder
                }
            }
            return "<not logged: not valid JSON, " + text.length() + " characters>";
        }
    }

    private static Object redactValue(Object value) {
        if (value instanceof JSONObject) {
            JSONObject redactedObject = new JSONObject(new LinkedHashMap<String, Object>());
            for (Map.Entry<String, Object> entry : ((JSONObject) value).entrySet()) {
                redactedObject.put(entry.getKey(), isSensitiveKey(entry.getKey()) ? REDACTED : redactValue(entry.getValue()));
            }
            return redactedObject;
        }
        if (value instanceof JSONArray) {
            JSONArray redactedArray = new JSONArray();
            for (Object item : (JSONArray) value) {
                redactedArray.add(redactValue(item));
            }
            return redactedArray;
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lowerCaseKey = key.toLowerCase(Locale.ROOT);
        for (String fragment : SENSITIVE_KEY_FRAGMENTS) {
            if (lowerCaseKey.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
