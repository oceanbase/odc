/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import com.google.common.base.Splitter;

import lombok.NonNull;

/**
 * Utils for map operations
 */
public class MapUtils {

    private static final String DEFAULT_ENTRY_SEPARATOR = ",";
    private static final String DEFAULT_KEY_VALUE_SEPARATOR = "=";

    /**
     * A util that maps a list of entries into a <K,V> map.
     */
    public static <K, V> Map<K, V> newMap(@NonNull Class<K> keyType, @NonNull Class<V> valueType, Object... entries) {
        if (entries == null || entries.length % 2 == 1) {
            throw new IllegalArgumentException("toMap must be called with an even number of parameters");
        }
        return IntStream.range(0, entries.length / 2).map(i -> i * 2).collect(HashMap::new,
                (m, i) -> m.put(keyType.cast(entries[i]), valueType.cast(entries[i + 1])), Map::putAll);
    }

    /**
     * build Map from string，use ',' for separate each k-v, use '=' for separate key and value
     *
     * input str example：
     * 
     * <pre>
     * null --> emptyMap
     * "" --> emptyMap
     * "a=1,b=2" --> map with 2 k-v
     * </pre>
     * 
     * @param str
     * @return
     */
    public static Map<String, String> fromKvString(String str) {
        return fromKvString(str, DEFAULT_ENTRY_SEPARATOR, DEFAULT_KEY_VALUE_SEPARATOR);
    }

    /**
     * build Map from string，use entrySeparator for separate each k-v, use keyToValueSeparator for
     * separate key and value
     * 
     * @param str input string
     * @param entrySeparator separator for each k-v, can't be blank
     * @param keyToValueSeparator separator for key and value, can't be blank
     * @return
     */
    public static Map<String, String> fromKvString(@NonNull String str, String entrySeparator,
            String keyToValueSeparator) {
        if (StringUtils.isBlank(entrySeparator) || StringUtils.isBlank(keyToValueSeparator)) {
            throw new IllegalArgumentException("entrySeparator and keyToValueSeparator can't be blank");
        }
        if (StringUtils.isEmpty(str)) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        for (String entry : Splitter.on(entrySeparator).split(str)) {
            String[] kvPair = org.apache.commons.lang3.StringUtils.split(entry, keyToValueSeparator, 2);
            map.put(kvPair[0], kvPair[1]);
        }
        return map;
    }

    /**
     * format map to kv structure, e.g. a=1,b=2
     */
    public static String formatKvString(@NonNull Map<String, String> map) {
        return formatKvString(map, DEFAULT_ENTRY_SEPARATOR, DEFAULT_KEY_VALUE_SEPARATOR);
    }

    /**
     * format map to kv structure, e.g. a=1,b=2
     * 
     * @param map map to format
     * @param entrySeparator separator for each k-v, can't be blank
     * @param keyToValueSeparator separator for key and value, can't be blank
     * @return
     */
    public static String formatKvString(@NonNull Map<String, String> map,
            String entrySeparator, String keyToValueSeparator) {
        if (StringUtils.isBlank(entrySeparator) || StringUtils.isBlank(keyToValueSeparator)) {
            throw new IllegalArgumentException("entrySeparator and keyToValueSeparator can't be blank");
        }
        map.forEach((k, v) -> {
            if (StringUtils.isEmpty(k) || StringUtils.containsAny(k, entrySeparator, keyToValueSeparator)) {
                throw new IllegalArgumentException("key can't be blank or contains separator, given key: " + k);
            }
            if (StringUtils.containsAny(v, entrySeparator, keyToValueSeparator)) {
                throw new IllegalArgumentException("value can't contains separator, given value: " + v);
            }
        });
        return org.apache.commons.lang3.StringUtils.join(
                map.entrySet().stream().map(
                        e -> e.getKey() + keyToValueSeparator + e.getValue()).toArray(),
                entrySeparator);
    }

    public static boolean isEmpty(final Map<?, ?> map) {
        return org.apache.commons.collections4.MapUtils.isEmpty(map);
    }

    public static int size(final Map<?, ?> map) {
        return org.apache.commons.collections4.MapUtils.size(map);
    }

    /**
     * compare map, null map and empty map consider as equals
     * 
     * @param src
     * @param target
     * @param <Key> key of map
     * @param <Value> value of map
     * @param equalFunction function with argument value to compare if value instance is equals
     * @return true is map is equals
     */
    public static <Key, Value> boolean isEqual(Map<Key, Value> src, Map<Key, Value> target,
            BiFunction<Value, Value, Boolean> equalFunction) {
        int currentSize = size(src);
        int targetSize = size(target);
        // map size not equals
        if (currentSize != targetSize) {
            return false;
        }
        if (currentSize == 0) {
            return true;
        }
        // check key map
        for (Map.Entry<Key, Value> entry : src.entrySet()) {
            if (!equalFunction.apply(entry.getValue(), target.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

}
