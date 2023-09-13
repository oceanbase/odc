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
package com.oceanbase.odc.service.flow.task;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.datatransfer.DataTransferService;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTaskContext;
import com.oceanbase.odc.service.flow.OdcInternalFileService;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Refer to import or export task
 *
 * @author wenniu.ly
 * @date 2022/2/18
 */

@Slf4j
public class DataTransferRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    @Autowired
    private DataTransferService dataTransferService;
    @Autowired
    private OdcInternalFileService odcInternalFileService;
    @Autowired
    private ConnectionTesting connectionTesting;
    private volatile DataTransferTaskContext context;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        if (context == null) {
            throw new IllegalStateException("Context is null, task may not be running");
        }
        boolean result = context.cancel(true);
        log.info("Data transfer task has been cancelled, taskId={}, result={}", taskId, result);
        taskService.cancel(taskId, DataTransferTaskResult.of(context));
        return true;
    }

    @Override
    public boolean isCancelled() {
        if (context == null) {
            return false;
        }
        return context.isCancelled();
    }

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
        log.info("Data transfer task starts, taskId={}", taskId);
        DataTransferParameter config = FlowTaskUtil.getDataTransferParameter(execution);
        config.setSchemaName(FlowTaskUtil.getSchemaName(execution));

        ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        config.setConnectionConfig(connectionConfig.simplify());
        injectSysConfig(connectionConfig, config);

        TaskEntity taskEntity = taskService.detail(taskId);
        ExecutorInfo executor = new ExecutorInfo(hostProperties);
        ExecutorInfo submitter = FlowTaskUtil.getTaskSubmitter(execution);
        if (taskEntity.getTaskType() == TaskType.IMPORT && submitter != null && !submitter.equals(executor)) {
            /**
             * 导入任务不在当前机器上，需要进行 {@code HTTP GET} 获取导入文件
             */
            odcInternalFileService.getExternalImportFiles(taskEntity, submitter, config.getImportFileName());
        }
        context = dataTransferService.create(taskId + "", config);
        taskService.start(taskId, DataTransferTaskResult.of(context));
        return null;
    }

    @Override
    protected boolean isSuccessful() {
        if (context == null || !context.isDone()) {
            return false;
        }
        try {
            context.get(1, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean isFailure() {
        if (context == null || !context.isDone()) {
            return false;
        }
        return !isSuccessful();
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Data transfer task failed, taskId={}", taskId);
        DataTransferTaskResult result = DataTransferTaskResult.of(context);
        if (context == null) {
            taskService.fail(taskId, 0, result);
        } else {
            taskService.fail(taskId, context.getProgress(), result);
        }
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Data transfer task succeed, taskId={}", taskId);
        try {
            taskService.succeed(taskId, context.get());
            updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        } catch (Exception e) {
            log.warn("Failed to get result", e);
        }
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Data transfer task timeout, taskId={}", taskId);
        taskService.fail(taskId, context.getProgress(), DataTransferTaskResult.of(context));
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (Objects.isNull(context)) {
            return;
        }
        taskService.updateProgress(taskId, context.getProgress());
    }

    private void injectSysConfig(ConnectionConfig connectionConfig, DataTransferParameter transferConfig) {
        if (StringUtils.isBlank(transferConfig.getSysUser())) {
            log.info("No Sys user setting, connectionId={}", connectionConfig.getId());
            return;
        }
        String sysUserInMeta = connectionConfig.getSysTenantUsername();
        String sysPasswdInMeta = connectionConfig.getSysTenantPassword();
        String sysUserInConfig = transferConfig.getSysUser();
        String sysPasswdInConfig = transferConfig.getSysPassword();
        if (sysPasswdInConfig == null) {
            if (sysPasswdInMeta == null) {
                log.info("No password for sys, connectionId={}", connectionConfig.getId());
                return;
            }
            Validate.isTrue(sysUserInConfig.equals(sysUserInMeta), "Sys user is illegal");
            if (!testSysTenantAccount(connectionConfig)) {
                log.warn("Access denied, Sys tenant account and password error, connectionId={}, sysUserInMeta={}",
                        connectionConfig.getId(), sysUserInMeta);
                throw new IllegalStateException("AccessDenied, " + sysUserInMeta);
            }
            return;
        }
        transferConfig.getConnectionConfig().setSysTenantUsername(sysUserInConfig);
        transferConfig.getConnectionConfig().setSysTenantPassword(sysPasswdInConfig);
        if (testSysTenantAccount(connectionConfig)) {
            log.info("Sys user has been approved, connectionId={}", connectionConfig.getId());
            return;
        }
        log.info("Access denied, Sys tenant account and password error, connectionId={}, sysUserInConfig={}",
                connectionConfig.getId(), sysUserInConfig);
        transferConfig.getConnectionConfig().setSysTenantUsername(null);
        transferConfig.getConnectionConfig().setSysTenantPassword(null);
    }

    private boolean testSysTenantAccount(ConnectionConfig connectionConfig) {
        TestConnectionReq req = TestConnectionReq.fromConnection(connectionConfig, ConnectionAccountType.SYS_READ);
        try {
            return connectionTesting.test(req).isActive();
        } catch (Exception e) {
            // eat exp
            return false;
        }
    }


}
