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

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link PartitionPlanConfig}
 *
 * @author yh263208
 * @date 2024-01-09 16:32
 * @since ODC_release_4.2.4
 */
@Getter
@Setter
@ToString
public class PartitionPlanConfig implements Serializable, TaskParameters {

    private static final long serialVersionUID = 7008051004183574287L;
    private Long id;
    private boolean enabled;
    private Long databaseId;
    private Long timeoutMillis;
    /**
     * (~, 0] -> ignore any errors (0, ~) -> meaningful value
     */
    private Integer maxErrors = -1;
    private TriggerConfig creationTrigger;
    private TriggerConfig droppingTrigger;
    private List<PartitionPlanTableConfig> partitionTableConfigs;

}
