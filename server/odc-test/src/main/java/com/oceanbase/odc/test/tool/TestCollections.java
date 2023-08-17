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
package com.oceanbase.odc.test.tool;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

/**
 * Utils for create collection test data
 */
public class TestCollections {
    private static final String DEFAULT_ENTRY_SEPARATOR = ",";
    private static final String DEFAULT_KEY_VALUE_SEPARATOR = "=";

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
     * @return Map<String,String>
     */
    public static Map<String, String> asMap(String str) {
        return asMap(str, DEFAULT_ENTRY_SEPARATOR, DEFAULT_KEY_VALUE_SEPARATOR);
    }

    public static Map<String, String> asMap(String str, String entrySeparator, String keyToValueSeparator) {
        if (StringUtils.isEmpty(str)) {
            return Collections.emptyMap();
        }
        return Splitter.on(entrySeparator).withKeyValueSeparator(keyToValueSeparator).split(str);
    }

}
