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

package com.oceanbase.odc.service.schedule.job;

import java.util.List;
import java.util.stream.Collectors;

import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.partitionplan.DatabasePartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.PartitionPlanTaskService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.PartitionPlanJobParameters;
import com.oceanbase.tools.migrator.common.exception.UnExpectedException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/8/21 10:37
 * @Descripition:
 */

@Slf4j
public class PartitionPlanJob implements OdcJob {

    private final PartitionPlanTaskService partitionPlanTaskService;

    private final PartitionPlanService partitionPlanService;

    private final ScheduleService scheduleService;



    public PartitionPlanJob() {

        partitionPlanTaskService = SpringContextUtil.getBean(PartitionPlanTaskService.class);

        partitionPlanService = SpringContextUtil.getBean(PartitionPlanService.class);

        scheduleService = SpringContextUtil.getBean(ScheduleService.class);
    }

    @Override
    public void execute(JobExecutionContext context) {
        Long scheduleId = ScheduleTaskUtils.getScheduleId(context);
        ScheduleEntity scheduleEntity;
        try {
            scheduleEntity = scheduleService.nullSafeGetById(scheduleId);
        } catch (Exception e) {
            log.warn("Schedule not found,scheduleId={}", scheduleId);
            return;
        }
        PartitionPlanJobParameters jobParameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                PartitionPlanJobParameters.class);

        DatabasePartitionPlanEntity databasePartitionPlan = partitionPlanService.getDatabasePartitionPlanById(
                jobParameters.getDatabasePartitionPlanId());

        List<TablePartitionPlanEntity> tablePartitionPlans =
                partitionPlanService
                        .getValidTablePlanByDatabasePartitionPlanId(jobParameters.getDatabasePartitionPlanId()).stream()
                        .filter(TablePartitionPlanEntity::getIsAutoPartition).collect(
                                Collectors.toList());

        if (!databasePartitionPlan.isConfigEnabled() || tablePartitionPlans.isEmpty()) {
            log.info(
                    "database partition plan is disable or no table need to process,start to stop partition-plan job,scheduleId={}",
                    scheduleId);
            try {
                scheduleService.terminate(scheduleEntity);
            } catch (SchedulerException e) {
                log.warn("Stop partition plan job failed.", e);
                throw new RuntimeException(e);
            }
        }
        try {
            partitionPlanTaskService.executePartitionPlan(databasePartitionPlan.getFlowInstanceId(),
                    tablePartitionPlans);
        } catch (Exception e) {
            log.warn("Create partition-plan database change task failed.", e);
        }
    }

    @Override
    public void interrupt() {
        throw new UnExpectedException();
    }
}
