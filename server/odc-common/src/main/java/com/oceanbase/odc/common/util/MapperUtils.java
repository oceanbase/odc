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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link MapperUtils}
 *
 * @author yh263208
 * @date 2022-04-20 20:36
 * @since ODC_release_3.3.1
 */
@Slf4j
public class MapperUtils {
    /**
     * {@link PathMatcher}
     *
     * @author yh263208
     * @date 2022-03-18 19:37
     * @since ODC_release_3.3.0
     */
    public interface PathMatcher {
        /**
         * Determine whether the target path has been found
         */
        boolean match(@NonNull List<Object> prefix, Object current);
    }

    public static <T> T get(@NonNull Map<?, ?> map, Class<T> clazz, @NonNull PathMatcher judger) {
        return get(map, new TypeReference<T>() {
            @Override
            public Type getType() {
                return clazz;
            }
        }, judger);
    }

    public static <T> T get(@NonNull Map<?, ?> map, @NonNull TypeReference<T> typeRef,
            @NonNull PathMatcher judger) {
        Object value = get(map, judger);
        if (value == null) {
            return null;
        }
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            String jsonStr = jsonMapper.writeValueAsString(value);
            return jsonMapper.readValue(jsonStr, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse from the map", e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T get(@NonNull Object obj, Class<T> clazz, @NonNull PathMatcher judger) {
        return get(obj, new TypeReference<T>() {
            @Override
            public Type getType() {
                return clazz;
            }
        }, judger);
    }

    public static <T> T get(@NonNull Object obj, @NonNull TypeReference<T> typeRef,
            @NonNull PathMatcher judger) {
        Object value = get(obj, judger);
        if (value == null) {
            return null;
        }
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            String jsonStr = jsonMapper.writeValueAsString(value);
            return jsonMapper.readValue(jsonStr, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse from the map", e);
            throw new RuntimeException(e);
        }
    }

    public static <T> Object get(@NonNull T target, @NonNull PathMatcher judger) {
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            String jsonStr = jsonMapper.writeValueAsString(target);
            Map<String, Object> map = jsonMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            return get(map, judger);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse from the map", e);
            throw new RuntimeException(e);
        }
    }

    public static Object get(@NonNull Map<?, ?> map, @NonNull PathMatcher judger) {
        if (judger.match(Collections.emptyList(), null)) {
            return map;
        }
        List<Object> objects = parseMap(map, new LinkedList<>(), judger);
        if (objects.size() == 0) {
            return null;
        }
        if (objects.size() == 1) {
            return objects.iterator().next();
        }
        return objects;
    }

    public static Object getOrDefault(@NonNull Map<?, ?> map, Object value, @NonNull PathMatcher judger) {
        Object returnVal = get(map, judger);
        if (returnVal == null) {
            return value;
        }
        return returnVal;
    }

    @SuppressWarnings("all")
    private static List<Object> parseMap(@NonNull Map<?, ?> map, LinkedList<Object> prefixes, PathMatcher judger) {
        List<Object> returnVal = new LinkedList<>();
        for (Object key : map.keySet()) {
            if (judger.match(new LinkedList<>(prefixes), key)) {
                returnVal.add(map.get(key));
            } else {
                Object value = map.get(key);
                prefixes.add(key);
                if (value instanceof Map) {
                    returnVal.addAll(parseMap((Map<Object, Object>) value, prefixes, judger));
                } else if (value instanceof Collection) {
                    returnVal.addAll(parseCollection((Collection<Object>) value, prefixes, judger));
                }
                prefixes.removeLast();
            }
        }
        return returnVal;
    }

    @SuppressWarnings("all")
    private static List<Object> parseCollection(@NonNull Collection<?> collection, LinkedList<Object> prefixes,
            PathMatcher judger) {
        List<Object> returnVal = new LinkedList<>();
        int counter = 0;
        for (Object item : collection) {
            Integer subKey = Integer.valueOf(counter);
            if (judger.match(new LinkedList<>(prefixes), subKey)) {
                returnVal.add(item);
            } else {
                prefixes.add(subKey);
                if (item instanceof Map) {
                    returnVal.addAll(parseMap((Map<Object, Object>) item, prefixes, judger));
                } else if (item instanceof Collection) {
                    returnVal.addAll(parseCollection((Collection<Object>) item, prefixes, judger));
                }
                prefixes.removeLast();
            }
            counter++;
        }
        return returnVal;
    }

}
