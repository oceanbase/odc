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
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.partitionplan.PartitionPlanScheduleService;
import com.oceanbase.odc.service.partitionplan.PartitionPlanServiceV2;
import com.oceanbase.odc.service.partitionplan.PartitionPlanTaskTraceContextHolder;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanPreViewResp;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
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
    private final PartitionPlanServiceV2 partitionPlanService;
    private final DatabaseService databaseService;
    private final PartitionPlanScheduleService partitionPlanScheduleService;

    public PartitionPlanJob() {
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.partitionPlanScheduleService = SpringContextUtil.getBean(PartitionPlanScheduleService.class);
        this.partitionPlanService = SpringContextUtil.getBean(PartitionPlanServiceV2.class);
    }

    @Override
    public void interrupt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void before(JobExecutionContext context) {}

    @Override
    public void after(JobExecutionContext context) {}

    @Override
    public void execute(JobExecutionContext context) {
        ScheduleEntity scheduleEntity;
        try {
            scheduleEntity = this.scheduleService.nullSafeGetById(ScheduleTaskUtils.getScheduleId(context));
        } catch (Exception e) {
            log.warn("Failed to get schedule by id", e);
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
            PartitionPlanConfig target = this.partitionPlanScheduleService.getPartitionPlan(
                    paramemters.getFlowInstanceId());
            if (!target.isEnabled()) {
                log.warn("Partition plan is disabled, partitionPlanId={}", partitionPlanId);
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
            ConnectionConfig conn = this.databaseService.findDataSourceForConnectById(paramemters.getDatabaseId());
            connectionSession = new DefaultConnectSessionFactory(conn).generateSession();
            List<PartitionPlanPreViewResp> resps = this.partitionPlanService.generatePartitionDdl(
                    connectionSession, tableConfigs, false);
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

}
