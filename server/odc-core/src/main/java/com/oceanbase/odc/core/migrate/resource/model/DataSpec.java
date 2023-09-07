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
package com.oceanbase.odc.core.migrate.resource.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link DataSpec}
 *
 * @author yh263208
 * @date 2022-04-20 11:07
 * @since ODC_release_3.3.0
 */
@Getter
@ToString(of = {"name", "value", "ignore"})
@EqualsAndHashCode(of = {"name", "value"})
public class DataSpec {
    private final String name;
    private final Object value;
    @Setter
    private boolean ignore;
    @Getter(AccessLevel.NONE)
    private final TableSpec entity;

    public DataSpec(@NonNull TableSpec entity, Object value) {
        this.name = entity.getName();
        this.value = value;
        this.ignore = entity.isIgnore();
        this.entity = entity;
    }

    public static DataSpec copyFrom(@NonNull DataSpec spec, Object value) {
        return new DataSpec(spec.entity, value);
    }

    public void refresh() {
        this.entity.setValue(value);
    }

}
