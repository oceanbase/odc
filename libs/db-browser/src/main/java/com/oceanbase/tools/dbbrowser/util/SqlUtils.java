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
package com.oceanbase.tools.dbbrowser.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author jingtian
 * @date
 * @since
 */
public class SqlUtils {

    private static final String ALL_CURRENT_TIMESTAMP_EXPRESSIONS_STR = "NOW(),"
            + "UTC_TIMESTAMP,UTC_TIMESTAMP(),LOCALTIME, LOCALTIME(),"
            + "LOCALTIMESTAMP,LOCALTIMESTAMP(),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP()";
    private static final Set<String> ALL_CURRENT_TIMESTAMP_EXPRESSIONS =
            newHashSet(ALL_CURRENT_TIMESTAMP_EXPRESSIONS_STR.split(","));

    /**
     * quote for mysql default value <br>
     * 1. if match current timestamp expression, skip quote <br>
     * 2. else quote with '' <br>
     *
     * @param value
     * @return quoted value if not current timestamp expression
     */
    public static String quoteMysqlDefaultValue(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (isCurrentTimestampExpression(value)) {
            return value;
        }
        return StringUtils.quoteMysqlValue(value);
    }

    public static boolean isCurrentTimestampExpression(String express) {
        if (StringUtils.isBlank(express)) {
            return false;
        }
        return ALL_CURRENT_TIMESTAMP_EXPRESSIONS.contains(express.toUpperCase());
    }

    @SafeVarargs
    private static <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

}
