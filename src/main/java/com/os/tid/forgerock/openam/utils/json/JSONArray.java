package com.os.tid.forgerock.openam.utils.json;

import java.util.ArrayList;
import java.util.List;

public class JSONArray extends ArrayList<Object> {
    private static final long serialVersionUID = 1L;

    public JSONObject getJSONObject(int index) {
        Object value = get(index);
        return value instanceof JSONObject ? (JSONObject) value : null;
    }

    public String getString(int index) {
        Object value = get(index);
        return value == null ? null : value.toString();
    }

    public <T> List<T> toJavaList(Class<T> clazz) {
        List<T> result = new ArrayList<>(size());
        for (Object element : this) {
            result.add(clazz == String.class ? clazz.cast(element == null ? null : element.toString()) : clazz.cast(element));
        }
        return result;
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
