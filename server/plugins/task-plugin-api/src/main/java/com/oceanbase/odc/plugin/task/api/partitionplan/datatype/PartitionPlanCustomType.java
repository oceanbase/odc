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
 * {@link PartitionPlanCustomType}
 *
 * @author yh263208
 * @date 2023-01-11 20:58
 * @since ODC_release_4.2.4
 * @see PartitionPlanDataType
 */
public class PartitionPlanCustomType implements PartitionPlanDataType {

    private final String name;

    public PartitionPlanCustomType(@NonNull String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Integer getPrecision() {
        return -1;
    }

    @Override
    public Integer getScale() {
        return -1;
    }

    @Override
    public Integer getWidth() {
        return -1;
    }

}
