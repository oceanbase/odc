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
package com.oceanbase.odc.plugin.task.api.partitionplan.datatype;

import lombok.NonNull;

/**
 * {@link PartitionPlanNumberType}
 *
 * @author yh263208
 * @date 2024-01-09 15:46
 * @since ODC_release_4.2.4
 */
public class PartitionPlanNumberType implements PartitionPlanDataType {

    private final Integer scale;
    private final Integer precision;

    public PartitionPlanNumberType(@NonNull Integer precision, @NonNull Integer scale) {
        this.scale = scale;
        this.precision = precision;
    }

    @Override
    public Integer getScale() {
        return this.scale;
    }

    @Override
    public Integer getWidth() {
        return -1;
    }

    @Override
    public String getName() {
        return "NUMBER";
    }

    @Override
    public Integer getPrecision() {
        return this.precision;
    }

}
