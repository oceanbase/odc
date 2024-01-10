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
package com.oceanbase.odc.service.partitionplan.model;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link PartitionPlanGeneralTimeType}
 *
 * @author yh263208
 * @date 2024-01-09 15:38
 * @since ODC_release_4.2.4
 * @see PartitionPlanDataType
 */
@Getter
public class PartitionPlanGeneralTimeType implements PartitionPlanDataType {

    private final TimeUnit precision;

    public PartitionPlanGeneralTimeType(@NonNull TimeUnit precision) {
        this.precision = precision;
    }

    @Override
    public String getName() {
        return "TIME";
    }

}
