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
package com.oceanbase.odc.core.flow.model;

import lombok.NonNull;

/**
 * Form type rnum for {@link org.flowable.engine.form.AbstractFormType}
 *
 * @author yh263208
 * @date 2022-01-18 20:26
 * @since ODC_release_3.3.0
 */
@SuppressWarnings("all")
public enum FlowableFormType {
    BOOLEAN_TYPE("boolean"),
    DATE_TYPE("date"),
    DOUBLE_TYPE("double"),
    ENUM_TYPE("enum"),
    LONG_TYPE("long"),
    STRING_TYPE("string");

    private final String name;

    FlowableFormType(@NonNull String name) {
        this.name = name;
    }

    public String getTypeName() {
        return this.name;
    }

}
