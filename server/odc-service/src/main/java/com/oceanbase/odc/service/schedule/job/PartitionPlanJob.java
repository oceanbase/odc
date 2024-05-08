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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.partitionplan.PartitionPlanScheduleService;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.PartitionPlanTaskTraceContextHolder;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanPreViewResp;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @author tinker
 * @author yh263208
 * @date 2024-02-21 17:36
 * @since ODC_release_4.2.4
 */
@Slf4j
public class PartitionPlanJob implements OdcJob {

    private final ScheduleService scheduleService;
    private final PartitionPlanService partitionPlanService;
    private final DatabaseService databaseService;
    private final FlowInstanceService flowInstanceService;
    private final PartitionPlanScheduleService partitionPlanScheduleService;

    public PartitionPlanJob() {
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.flowInstanceService = SpringContextUtil.getBean(FlowInstanceService.class);
        this.partitionPlanScheduleService = SpringContextUtil.getBean(PartitionPlanScheduleService.class);
        this.partitionPlanService = SpringContextUtil.getBean(PartitionPlanService.class);
    }

    @Override
    public void before(JobExecutionContext context) {}

    @Override
    public void after(JobExecutionContext context) {}

    @Override
    public void interrupt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(JobExecutionContext context) {
        ScheduleEntity scheduleEntity;
        try {
            scheduleEntity = this.scheduleService.nullSafeGetById(ScheduleTaskUtils.getScheduleId(context));
        } catch (Exception e) {
            return;
        }
        PartitionPlanConfig paramemters = JsonUtils.fromJson(
                scheduleEntity.getJobParametersJson(), PartitionPlanConfig.class);
        Long partitionPlanId = paramemters.getId();
        ConnectionSession connectionSession = null;
        PartitionPlanTaskTraceContextHolder.trace(paramemters.getTaskId());
        try {
            if (CollectionUtils.isEmpty(paramemters.getPartitionTableConfigs())) {
                log.warn("Failed to get any partition plan tables, partitionPlanId={}", partitionPlanId);
                return;
            }
            PartitionPlanConfig target = this.partitionPlanScheduleService.getPartitionPlanByFlowInstanceId(
                    paramemters.getFlowInstanceId());
            if (target == null || !target.isEnabled()) {
                log.warn("Partition plan is null or disabled, partitionPlanId={}", partitionPlanId);
                return;
            }
            Set<Long> tableConfigIds = paramemters.getPartitionTableConfigs().stream()
                    .map(PartitionPlanTableConfig::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            List<PartitionPlanTableConfig> tableConfigs = target.getPartitionTableConfigs().stream()
                    .filter(tableConfig -> tableConfigIds.contains(tableConfig.getId()) && tableConfig.isEnabled())
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(tableConfigs)) {
                log.warn("Failed to get any enabled partition plan tables, partitionPlanId={}", partitionPlanId);
                return;
            }
            Database database = this.databaseService.getBasicSkipPermissionCheck(target.getDatabaseId());
            ConnectionConfig conn = this.databaseService.findDataSourceForConnectById(target.getDatabaseId());
            conn.setDefaultSchema(database.getName());
            connectionSession = new DefaultConnectSessionFactory(conn).generateSession();
            List<PartitionPlanPreViewResp> resps = this.partitionPlanService.generatePartitionDdl(
                    connectionSession, tableConfigs, false);
            submitSubDatabaseChangeTask(paramemters.getFlowInstanceId(), target.getDatabaseId(),
                    resps.stream().flatMap(i -> i.getSqls().stream()).collect(Collectors.toList()),
                    paramemters.getTimeoutMillis(), paramemters.getErrorStrategy());
        } catch (Exception e) {
            log.warn("Failed to execute a partition plan task", e);
        } finally {
            try {
                if (connectionSession != null) {
                    connectionSession.expire();
                }
            } catch (Exception e) {
                // eat exception
            }
            PartitionPlanTaskTraceContextHolder.clear();
        }
    }

    private void submitSubDatabaseChangeTask(Long parentFlowInstanceId, Long databaseId,
            List<String> sqls, long timeoutMillis, TaskErrorStrategy errorStrategy) {
        if (CollectionUtils.isEmpty(sqls)) {
            return;
        }
        DatabaseChangeParameters taskParameters = new DatabaseChangeParameters();
        taskParameters.setModifyTimeoutIfTimeConsumingSqlExists(false);
        taskParameters.setErrorStrategy(errorStrategy.name());
        StringBuilder sqlContent = new StringBuilder();
        for (String sql : sqls) {
            sqlContent.append(sql).append("\n");
        }
        taskParameters.setSqlContent(sqlContent.toString());
        taskParameters.setTimeoutMillis(timeoutMillis);
        taskParameters.setParentJobType(JobType.PARTITION_PLAN);
        CreateFlowInstanceReq flowInstanceReq = new CreateFlowInstanceReq();
        flowInstanceReq.setParameters(taskParameters);
        flowInstanceReq.setTaskType(TaskType.ASYNC);
        flowInstanceReq.setDatabaseId(databaseId);
        flowInstanceReq.setParentFlowInstanceId(parentFlowInstanceId);
        flowInstanceReq.setExecutionStrategy(FlowTaskExecutionStrategy.AUTO);
        List<FlowInstanceDetailResp> flowInstance = this.flowInstanceService.createWithoutApprovalNode(
                flowInstanceReq);
        if (flowInstance.isEmpty()) {
            log.warn("Create partition plan subtask failed.");
        } else {
            log.info("Create partition plan subtask success,flowInstanceId={}", flowInstance.get(0).getId());
        }
    }

}
