package com.os.tid.forgerock.openam.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.NullPredicate;
import org.forgerock.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CollectionsUtils {
    private static final Logger logger = LoggerFactory.getLogger("amAuth");

    private CollectionsUtils() {
    }

    public static <K, V> boolean hasAnyNullValues(Map<K, V> map) {
        return CollectionUtils.exists(map.values(), NullPredicate.INSTANCE);
    }

    public static boolean hasAnyNullValues(List<JsonValue> list) {
        for (JsonValue jsonValue: list) {
            if(jsonValue.isNull() || !jsonValue.isString()){return true;}
        }
        return false;
    }

}
