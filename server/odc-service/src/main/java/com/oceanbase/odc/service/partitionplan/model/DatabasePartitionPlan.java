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

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author：tinker
 * @Date: 2022/9/20 21:25
 * @Descripition:
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabasePartitionPlan implements Serializable {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;
    private Long flowInstanceId;
    private Long connectionId;
    private Long databaseId;
    private boolean inspectEnable;
    private InspectTriggerStrategy inspectTriggerStrategy;
    private List<TablePartitionPlan> tablePartitionPlans;
    private TriggerConfig triggerConfig;
}
