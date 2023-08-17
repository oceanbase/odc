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
package com.oceanbase.odc.service.common.util;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;

/**
 * constants for data access layer
 */
public interface EmptyValues {

    /**
     * null or empty match value for String type
     */
    String EXPRESSION = "{empty}";

    /**
     * null match value for Long type, <br>
     * different from null value, null value will be ignored from where condition
     */
    Long LONG_VALUE = 0L;

    String STRING_VALUE = "";

    static <T> boolean matches(T value) {
        if (value instanceof String) {
            return EXPRESSION.equals(value);
        }
        if (value instanceof Long) {
            return LONG_VALUE.equals(value);
        }
        if (value instanceof ConnectType) {
            return false;
        }
        if (value instanceof DialectType) {
            return false;
        }
        throw new UnexpectedException(
                "unsupported value type for empty expression judge, valueType=" + value.getClass().getName());
    }
}
