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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;
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
public class PartitionPlanConfig implements Serializable, TaskParameters, ScheduleTaskParameters {

    private static final long serialVersionUID = 7008051004183574287L;
    private Long id;
    private boolean enabled;
    private Long databaseId;
    private Long flowInstanceId;
    private Long taskId;
    private Long timeoutMillis;
    private TaskErrorStrategy errorStrategy = TaskErrorStrategy.CONTINUE;
    private TriggerConfig creationTrigger;
    private TriggerConfig droppingTrigger;
    private List<PartitionPlanTableConfig> partitionTableConfigs;

    @JsonProperty(access = Access.READ_ONLY)
    public List<Date> getCreateTriggerNextFireTimes() {
        if (this.creationTrigger == null) {
            return Collections.emptyList();
        }
        try {
            return QuartzCronExpressionUtils.getNextFiveFireTimes(
                    this.creationTrigger.getCronExpression());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @JsonProperty(access = Access.READ_ONLY)
    public List<Date> getDropTriggerNextFireTimes() {
        if (this.droppingTrigger == null) {
            return Collections.emptyList();
        }
        try {
            return QuartzCronExpressionUtils.getNextFiveFireTimes(
                    this.droppingTrigger.getCronExpression());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
