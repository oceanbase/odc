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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.config.OrganizationConfigUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanPreViewResp;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.base.sqlplan.SqlExecuteTask;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.util.JobUtils;

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
    public final ConnectProperties connectProperties;
    public final ConnectionService datasourceService;
    public final JobScheduler jobScheduler;
    private final ScheduleTaskService scheduleTaskService;
    public final OrganizationConfigUtils organizationConfigUtils;

    public PartitionPlanJob() {
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.partitionPlanService = SpringContextUtil.getBean(PartitionPlanService.class);
        this.connectProperties = SpringContextUtil.getBean(ConnectProperties.class);
        this.datasourceService = SpringContextUtil.getBean(ConnectionService.class);
        this.jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        this.scheduleTaskService = SpringContextUtil.getBean(ScheduleTaskService.class);
        this.organizationConfigUtils = SpringContextUtil.getBean(OrganizationConfigUtils.class);
    }


    @Override
    public void execute(JobExecutionContext context) {
        ScheduleEntity scheduleEntity;
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        try {
            scheduleEntity = this.scheduleService.nullSafeGetById(ScheduleTaskUtils.getScheduleId(context));
        } catch (Exception e) {
            return;
        }
        PartitionPlanConfig parameters = JsonUtils.fromJson(
                scheduleEntity.getJobParametersJson(), PartitionPlanConfig.class);
        List<PartitionPlanTableConfig> tableConfigs =
                partitionPlanService.getTableConfigsByScheduleId(scheduleEntity.getId());

        if (CollectionUtils.isEmpty(tableConfigs)) {
            log.warn("Failed to get any enabled partition plan tables, schedule id={}", scheduleEntity.getId());
            return;
        }
        ConnectionSession connectionSession = null;
        try {
            Database database = this.databaseService.getBasicSkipPermissionCheck(scheduleEntity.getDatabaseId());
            ConnectionConfig dataSource = datasourceService.getDecryptedConfig(database.getDataSource().getId());
            dataSource.setDefaultSchema(database.getName());
            connectionSession = new DefaultConnectSessionFactory(dataSource).generateSession();
            List<PartitionPlanPreViewResp> resps = this.partitionPlanService.generatePartitionDdl(
                    connectionSession, tableConfigs, false);
            submitSubSqlExecuteTask(dataSource, taskEntity.getId(),
                    resps.stream().flatMap(i -> i.getSqls().stream()).collect(Collectors.toList()),
                    parameters.getTimeoutMillis(), parameters.getErrorStrategy());
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
        }
    }

    private void submitSubSqlExecuteTask(ConnectionConfig dataSource, Long taskId,
            List<String> sqls, long timeoutMillis, TaskErrorStrategy errorStrategy) {
        if (CollectionUtils.isEmpty(sqls)) {
            return;
        }
        StringBuilder sqlContent = new StringBuilder();
        for (String sql : sqls) {
            sqlContent.append(sql).append("\n");
        }
        PublishSqlExecuteJobReq parameters = new PublishSqlExecuteJobReq();
        parameters.setSqlContent(sqlContent.toString());
        parameters.setRetryTimes(0);
        parameters.setDelimiter(";");
        parameters.setQueryLimit(1000);
        parameters.setTimeoutMillis(timeoutMillis);
        parameters.setErrorStrategy(errorStrategy);
        parameters.setSessionTimeZone(connectProperties.getDefaultTimeZone());
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.CONNECTION_CONFIG, JobUtils.toJson(dataSource));
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, JobUtils.toJson(parameters));
        jobData.put(JobParametersKeyConstants.DEFAULT_MAX_QUERY_LIMIT,
                organizationConfigUtils.getDefaultMaxQueryLimit().toString());

        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(1);

        Map<String, String> jobProperties = new HashMap<>(singleJobProperties.toJobProperties());
        DefaultJobDefinition jd = DefaultJobDefinition.builder().jobClass(SqlExecuteTask.class)
                .jobType("PARTITION_PLAN")
                .jobParameters(jobData)
                .jobProperties(jobProperties)
                .build();

        Long jobId = jobScheduler.scheduleJobNow(jd);

        scheduleTaskService.updateJobIdByTaskIdWithCheckScheduleTaskCancelingStatus(taskId, jobId);
        log.info("Publish partition plan database change job to task framework success, scheduleTaskId={}, jobId={}",
                taskId,
                jobId);
    }

    @Override
    public void before(JobExecutionContext context) {
        scheduleService.refreshScheduleStatus(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void after(JobExecutionContext context) {
        scheduleService.refreshScheduleStatus(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void interrupt() {
        throw new UnsupportedException();
    }


}
