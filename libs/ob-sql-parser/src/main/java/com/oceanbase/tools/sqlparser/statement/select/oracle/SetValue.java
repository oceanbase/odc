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
package com.oceanbase.tools.sqlparser.statement.select.oracle;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link SetValue}
 *
 * @author yh263208
 * @date 2022-12-07 16:59
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode
public class SetValue {

    private final String name;
    private final String value;
    private final String defaultValue;

    public SetValue(@NonNull String name, String value, String defaultValue) {
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SET");
        builder.append(" ").append(this.name);
        if (this.value != null) {
            builder.append(" TO ").append(this.value);
        }
        if (this.defaultValue != null) {
            builder.append(" DEFAULT ").append(this.defaultValue);
        }
        return builder.toString();
    }

}
