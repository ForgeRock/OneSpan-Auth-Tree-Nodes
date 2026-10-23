package com.os.tid.forgerock.openam.utils.json;

import java.util.LinkedHashMap;
import java.util.Map;

public class JSONObject extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public JSONObject() {
        super();
    }

    public JSONObject(Map<String, Object> map) {
        super(map);
    }

    public String getString(String key) {
        Object value = get(key);
        return value == null ? null : value.toString();
    }

    public JSONObject getJSONObject(String key) {
        Object value = get(key);
        return value instanceof JSONObject ? (JSONObject) value : null;
    }

    public JSONArray getJSONArray(String key) {
        Object value = get(key);
        return value instanceof JSONArray ? (JSONArray) value : null;
    }

    public Integer getInteger(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(value.toString().trim());
    }

    public int getIntValue(String key) {
        Integer value = getInteger(key);
        return value == null ? 0 : value;
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
