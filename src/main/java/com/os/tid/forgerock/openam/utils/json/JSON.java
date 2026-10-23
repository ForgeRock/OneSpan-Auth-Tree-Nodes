package com.os.tid.forgerock.openam.utils.json;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Jackson-backed replacement for the fastjson entry points used by the nodes (OPENAM-29243 / OPENAM-25860).
 * Parsing never instantiates arbitrary classes: only plain JSONObject/JSONArray/String/Number/Boolean/null are produced.
 */
public final class JSON {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build()
            .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

    private JSON() {
    }

    public static Object parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return toPlain(MAPPER.readTree(text));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    public static JSONObject parseObject(String text) {
        Object parsed = parse(text);
        if (parsed == null) {
            return null;
        }
        if (parsed instanceof JSONObject) {
            return (JSONObject) parsed;
        }
        throw new IllegalArgumentException("JSON text is not an object");
    }

    public static <T> T toJavaObject(JSONObject json, Class<T> clazz) {
        return MAPPER.convertValue(json, clazz);
    }

    public static String toJSONString(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Unable to serialize to JSON: " + e.getMessage(), e);
        }
    }

    private static Object toPlain(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            JSONObject object = new JSONObject();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                object.put(field.getKey(), toPlain(field.getValue()));
            }
            return object;
        }
        if (node.isArray()) {
            JSONArray array = new JSONArray();
            for (JsonNode element : node) {
                array.add(toPlain(element));
            }
            return array;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.canConvertToInt() ? (Object) node.intValue() : node.canConvertToLong() ? (Object) node.longValue() : node.bigIntegerValue();
        }
        return node.decimalValue();
    }
}
