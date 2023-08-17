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
package com.oceanbase.odc.service.onlineschemachange;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBObjectOperators;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.model.LinkType;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.pipeline.BaseCreateOmsProjectValve;
import com.oceanbase.odc.service.onlineschemachange.pipeline.DefaultLinkPipeline;
import com.oceanbase.odc.service.onlineschemachange.pipeline.OscValveContext;
import com.oceanbase.odc.service.onlineschemachange.pipeline.Pipeline;
import com.oceanbase.odc.service.onlineschemachange.pipeline.ScheduleCheckOmsProjectValve;
import com.oceanbase.odc.service.onlineschemachange.pipeline.SwapTableNameValve;
import com.oceanbase.odc.service.onlineschemachange.subtask.OscTaskCompleteHandler;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultOnlineSchemaChangeTaskHandler
 *
 * @author yaobin
 * @date 2023-06-08
 * @since 4.2.0
 */
@Slf4j
@Component
public class DefaultOnlineSchemaChangeTaskHandler implements OnlineSchemaChangeTaskHandler {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ProjectOpenApiService projectOpenApiService;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private OscTaskCompleteHandler completeHandler;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    private Map<LinkType, Pipeline> preparePipelineMap;
    private Map<LinkType, Pipeline> completePipelineMap;

    @PostConstruct
    public void init() {
        Pipeline preparePipeline = new DefaultLinkPipeline();
        preparePipeline.setBasic(SpringContextUtil.getBean(BaseCreateOmsProjectValve.class));
        Map<LinkType, Pipeline> prepareLinks = new HashMap<>();
        prepareLinks.put(LinkType.OMS, preparePipeline);
        this.preparePipelineMap = Collections.unmodifiableMap(prepareLinks);

        Pipeline completePipeline = new DefaultLinkPipeline();
        completePipeline.addValve(SpringContextUtil.getBean(ScheduleCheckOmsProjectValve.class));
        completePipeline.setBasic(SpringContextUtil.getBean(SwapTableNameValve.class));
        Map<LinkType, Pipeline> completeLinks = new HashMap<>();
        completeLinks.put(LinkType.OMS, completePipeline);
        this.completePipelineMap = Collections.unmodifiableMap(completeLinks);
    }

    @Override
    public void start(@NonNull Long scheduleId, @NonNull Long scheduleTaskId) {
        log.info("Start execute {}, to start schedule task id {}", getClass().getSimpleName(), scheduleTaskId);
        OscValveContext valveContext = null;
        try {
            valveContext = getOscValveContext(scheduleId, scheduleTaskId);
        } catch (Exception ex) {
            completeHandler.updateScheduleTask(scheduleTaskId, TaskStatus.FAILED);
            throw new IllegalArgumentException("Failed to start osc job with scheduleTaskId " + scheduleTaskId, ex);
        }
        completeHandler.updateScheduleTask(scheduleTaskId, TaskStatus.RUNNING);
        doStart(scheduleTaskId, valveContext);
    }

    private void doStart(Long scheduleTaskId, OscValveContext valveContext) {
        ConnectionSession connectionSession = null;
        try {
            connectionSession =
                    new DefaultConnectSessionFactory(valveContext.getConnectionConfig()).generateSession();
            ConnectionSessionUtil.setCurrentSchema(connectionSession,
                    valveContext.getTaskParameter().getDatabaseName());
            prepareSchema(valveContext.getParameter(), valveContext.getTaskParameter(),
                    connectionSession, scheduleTaskId);

            valveContext.setConnectionSession(connectionSession);
            valveContext.setLinkType(LinkType.OMS);
            preparePipelineMap.get(LinkType.OMS).invoke(valveContext);

        } catch (Exception e) {
            log.error("Failed to start osc job with taskId={}.", scheduleTaskId, e);
            completeHandler.proceed(valveContext, TaskStatus.FAILED, valveContext.getParameter().isContinueOnError());
        } finally {
            if (connectionSession != null) {
                connectionSession.expire();
            }
        }
    }

    @Override
    public void complete(@NonNull Long scheduleId) {
        log.debug("Start execute {}, to complete schedule id {}", getClass().getSimpleName(), scheduleId);
        Optional<ScheduleTaskEntity> task = findFirstRunningSchedule(scheduleId);
        if (task.isPresent()) {
            doComplete(scheduleId, task.get());
        } else {
            log.info("All schedule tasks in schedule id {} has completed, "
                    + "preparing delete current quartz job", scheduleId);
            completeHandler.deleteQuartzJob(scheduleId, JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        }
    }

    private void doComplete(Long scheduleId, ScheduleTaskEntity task) {
        Long scheduleTaskId = task.getId();
        log.info("schedule id {} to check schedule task status with schedule task id {}",
                scheduleId, scheduleTaskId);
        OscValveContext valveContext = getOscValveContext(scheduleId, scheduleTaskId);
        try {
            completePipelineMap.get(LinkType.OMS).invoke(valveContext);
            if (valveContext.isSwapSucceedCallBack()) {
                completeHandler.proceed(valveContext, TaskStatus.DONE, true);
            }
        } catch (Exception e) {
            log.warn("Failed to complete osc job with scheduleTaskId {}.", scheduleTaskId, e);
            completeHandler.proceed(valveContext, TaskStatus.FAILED,
                    valveContext.getParameter().isContinueOnError());
        }
    }

    @Override
    public void terminate(@NonNull Long scheduleTaskId) {
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        String jobName = scheduleTaskEntity.getJobName();
        OscValveContext valveContext = createValveContext(Long.parseLong(jobName), scheduleTaskId);
        completeHandler.proceed(valveContext, TaskStatus.CANCELED, false);
    }

    private OscValveContext getOscValveContext(Long scheduleId, Long scheduleTaskId) {
        OscValveContext valveContext = null;
        try {
            valveContext = createValveContext(scheduleId, scheduleTaskId);
            ValidatorUtils.verifyField(valveContext.getTaskParameter());
        } catch (Exception e) {
            log.warn("Failed to create valve context , schedule task id " + scheduleTaskId,
                    e);
            throw new IllegalArgumentException("Failed to create valve context , schedule task id \" + "
                    + "scheduleTaskId,\n", e);
        }
        return valveContext;
    }

    private Optional<ScheduleTaskEntity> findFirstRunningSchedule(Long scheduleId) {
        Specification<ScheduleTaskEntity> specification = Specification
                .where(ScheduleTaskSpecs.jobNameEquals(scheduleId + ""));
        return scheduleTaskRepository.findAll(specification, Sort.by("id"))
                .stream()
                .filter(taskEntity -> taskEntity.getStatus() == TaskStatus.RUNNING)
                .findFirst();
    }

    private OscValveContext createValveContext(Long scheduleId, Long scheduleTaskId) {
        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(scheduleId);
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                scheduleEntity.getJobParametersJson(), OnlineSchemaChangeParameters.class);
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters = JsonUtils.fromJson(
                scheduleTaskEntity.getParametersJson(), OnlineSchemaChangeScheduleTaskParameters.class);

        OscValveContext valveContext = new OscValveContext();
        valveContext.setSchedule(scheduleEntity);
        valveContext.setScheduleTask(scheduleTaskEntity);
        valveContext.setParameter(onlineSchemaChangeParameters);
        valveContext.setTaskParameter(oscScheduleTaskParameters);

        ConnectionConfig config =
                connectionService.getForConnectionSkipPermissionCheck(scheduleEntity.getConnectionId());
        config.setDefaultSchema(valveContext.getTaskParameter().getDatabaseName());
        valveContext.setConnectionConfig(config);
        return valveContext;
    }

    private void prepareSchema(OnlineSchemaChangeParameters param, OnlineSchemaChangeScheduleTaskParameters taskParam,
            ConnectionSession session, Long scheduleTaskId) throws SQLException {

        List<String> list = DBSchemaAccessors.create(session)
                .showTablesLike(taskParam.getDatabaseName(), taskParam.getNewTableName());
        // Drop new table suffix with _osc_new_ if exists
        if (CollectionUtils.isNotEmpty(list)) {
            DBObjectOperators.create(session)
                    .drop(DBObjectType.TABLE, taskParam.getDatabaseName(), taskParam.getNewTableName());
        }

        SyncJdbcExecutor executor = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        executor.execute(taskParam.getNewTableCreateDdl());
        if (param.getSqlType() == OnlineSchemaChangeSqlType.ALTER) {
            taskParam.getSqlsToBeExecuted().forEach(executor::execute);

            // update new table ddl for display
            String finalTableDdl = DdlUtils.queryOriginTableCreateDdl(session, taskParam.getNewTableName());
            String ddlForDisplay = DdlUtils.replaceTableName(finalTableDdl, taskParam.getOriginTableName(),
                    session.getDialectType(), param.getSqlType());
            taskParam.setNewTableCreateDdlForDisplay(ddlForDisplay);
            scheduleTaskRepository.updateTaskResult(scheduleTaskId,
                    JsonUtils.toJson(new OnlineSchemaChangeScheduleTaskResult(taskParam)));
        }
        log.info("Successfully created new table, ddl: {}", taskParam.getNewTableCreateDdl());
    }

}
