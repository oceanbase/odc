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
    public static <K, V> Map<K, V> newMap(Class<K> keyType, Class<V> valueType, Object... entries) {
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

    public static Map<String, String> fromKvString(String str, String entrySeparator, String keyToValueSeparator) {
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

    public static String formatKvString(@NonNull Map<String, String> map,
            String entrySeparator, String keyToValueSeparator) {
        return org.apache.commons.lang3.StringUtils.join(
                map.entrySet().stream().map(e -> e.getKey() + keyToValueSeparator + e.getValue()).toArray(),
                entrySeparator);
    }

    public static boolean isEmpty(final Map<?, ?> map) {
        return org.apache.commons.collections4.MapUtils.isEmpty(map);
    }

    public static int size(final Map<?, ?> map) {
        return org.apache.commons.collections4.MapUtils.size(map);
    }

}
