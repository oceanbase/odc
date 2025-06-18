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

import java.util.Map;
import java.util.stream.Collectors;

import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMConfiguration;
import com.oceanbase.odc.service.dlm.DLMService;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.TaskDescription;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/26 20:05
 * @Descripition:
 */
@Slf4j
public abstract class AbstractDlmJob extends AbstractOdcJob {

    public final ScheduleTaskRepository scheduleTaskRepository;
    public final ScheduleTaskService scheduleTaskService;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final DlmLimiterService limiterService;
    public final DLMService dlmService;

    public final TaskFrameworkService taskFrameworkService;

    public final DLMConfiguration dlmConfiguration;


    public AbstractDlmJob() {
        scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        scheduleTaskService = SpringContextUtil.getBean(ScheduleTaskService.class);
        databaseService = SpringContextUtil.getBean(DatabaseService.class);
        scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        limiterService = SpringContextUtil.getBean(DlmLimiterService.class);
        taskFrameworkService = SpringContextUtil.getBean(TaskFrameworkService.class);
        dlmService = SpringContextUtil.getBean(DLMService.class);
        dlmConfiguration = SpringContextUtil.getBean(DLMConfiguration.class);
    }

    public DataSourceInfo getDataSourceInfo(Long databaseId) {
        Database db = databaseService.detail(databaseId);
        ConnectionConfig config = databaseService.findDataSourceForTaskById(databaseId);
        DataSourceInfo dataSourceInfo = DataSourceInfoMapper.toDataSourceInfo(config, db.getName());
        dataSourceInfo.setDatabaseName(db.getName());
        dataSourceInfo.setSessionLimitRatio(dlmConfiguration.getSessionLimitingRatio());
        dataSourceInfo.setEnabledLimit(dlmConfiguration.isSessionLimitingEnabled());
        return dataSourceInfo;
    }

    public Long publishJob(DLMJobReq params, Long timeoutMillis, Long srcDatabaseId) {
        return submitToTaskFramework(JsonUtils.toJson(params), TaskDescription.DLM.name(), timeoutMillis,
                srcDatabaseId);
    }

    public DLMJobReq getDLMJobReqWhenRetry(Long jobId) {
        DLMJobReq dlmJobReq = JsonUtils.fromJson(JsonUtils.fromJson(
                taskFrameworkService.find(jobId).getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}).get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobReq.class);
        Map<String, DlmTableUnit> tableName2Unit =
                dlmService.findByScheduleTaskId(dlmJobReq.getScheduleTaskId()).stream()
                        .collect(
                                Collectors.toMap(DlmTableUnit::getTableName, o -> o));
        dlmJobReq.getTables().forEach(o -> o.setLastProcessedStatus(tableName2Unit.get(o.getTableName()).getStatus()));
        return dlmJobReq;
    }

    public DLMJobReq getDLMJobReqWithArchiveRange(Long jobId) {
        DLMJobReq dlmJobReq = JsonUtils.fromJson(JsonUtils.fromJson(
                taskFrameworkService.find(jobId).getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}).get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobReq.class);
        Map<String, DlmTableUnit> tableName2Unit =
                dlmService.findByScheduleTaskId(dlmJobReq.getScheduleTaskId()).stream()
                        .collect(
                                Collectors.toMap(DlmTableUnit::getTableName, o -> o));
        dlmJobReq.getTables().forEach(o -> {
            if (tableName2Unit.containsKey(o.getTableName())
                    && tableName2Unit.get(o.getTableName()).getStatistic() != null) {
                o.setPartName2MinKey(tableName2Unit.get(o.getTableName()).getStatistic().getPartName2MinKey());
                o.setPartName2MaxKey(tableName2Unit.get(o.getTableName()).getStatistic().getPartName2MaxKey());
            }
        });

        return dlmJobReq;
    }


    @Override
    public void execute(JobExecutionContext context) {
        if (context.getResult() == null) {
            log.warn("Concurrent execute is not allowed,job will be existed.jobKey={}",
                    context.getJobDetail().getKey());
            return;
        }
        executeJob(context);
    }

    public abstract void executeJob(JobExecutionContext context);


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

    }

}
