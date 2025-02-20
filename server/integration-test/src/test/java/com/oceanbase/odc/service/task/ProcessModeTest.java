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
package com.oceanbase.odc.service.task;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.plugin.PluginProperties;
import com.oceanbase.odc.service.task.base.databasechange.DatabaseChangeTask;
import com.oceanbase.odc.service.task.caller.ExecutorProcessBuilderFactory;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentFactory;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-22
 * @since 4.2.4
 */
@Ignore("manual test this case for process mode")
@Slf4j
public class ProcessModeTest extends BaseJobTest {

    private Long exceptedTaskId;

    @Before
    public void before() {
        exceptedTaskId = System.currentTimeMillis();
    }

    @Test
    public void test_start_task_process_mode() throws IOException {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        String processId = runtimeMxBean.getName(); // return "pid@hostname"
        String pid = processId.split("@")[0]; // get process id

        log.info("Current Java PID: {}", pid);

        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setEnvironments(getEnvironments());
        processConfig.setJvmXmxMB(512);
        processConfig.setJvmXmsMB(256);
        String executorName =
                JobUtils.generateExecutorName(JobIdentity.of(exceptedTaskId), new Date(System.currentTimeMillis()));
        ProcessBuilder pb = new ExecutorProcessBuilderFactory()
                .getProcessBuilder(processConfig, exceptedTaskId, executorName);
        Process process = null;
        try {
            process = pb.start();
            long executorPid = SystemUtils.getProcessPid(process);
            Assert.assertNotEquals(-1, executorPid);
            boolean isRunning = SystemUtils.isProcessRunning(executorPid,
                    JobUtils.generateExecutorSelectorOnProcess(executorName));
            Assert.assertTrue(isRunning);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private Map<String, String> getEnvironments() {
        Map<String, String> environments = new HashMap<>();
        String pluginPath = Paths.get("").toAbsolutePath().getParent().getParent()
                .resolve("distribution/plugins").toFile().getAbsolutePath();
        environments.put(PluginProperties.PLUGIN_DIR_KEY, pluginPath);
        environments.put(JobEnvKeyConstants.REPORT_ENABLED, Boolean.FALSE.toString());

        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        JobDefinition jd = buildJobDefinition();
        JobContext jc = new DefaultJobContextBuilder().build(jobIdentity, jd);
        Map<String, String> envMap =
                new JobEnvironmentFactory().build(jc, TaskRunMode.PROCESS, LogUtils.getBaseLogPath());
        JobUtils.encryptEnvironments(envMap);

        environments.putAll(envMap);
        return environments;
    }

    private JobDefinition buildJobDefinition() {
        DatabaseChangeParameters parameters = new DatabaseChangeParameters();
        parameters.setSqlContent(String.format("CREATE TABLE %s (id int(10))", "t_" + exceptedTaskId));
        parameters.setErrorStrategy(TaskErrorStrategy.ABORT.name());
        PreConditions.validArgumentState(
                parameters.getSqlContent() != null || CollectionUtils.isNotEmpty(parameters.getSqlObjectIds()),
                ErrorCodes.BadArgument, new Object[] {"sql"}, "input sql is empty");

        ConnectionConfig config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, JsonUtils.toJson(parameters));
        jobData.put(JobParametersKeyConstants.CONNECTION_CONFIG, JobUtils.toJson(config));
        jobData.put(JobParametersKeyConstants.FLOW_INSTANCE_ID, exceptedTaskId + "");
        jobData.put(JobParametersKeyConstants.CURRENT_SCHEMA, config.getDefaultSchema());
        jobData.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS, 30 * 60 * 1000 + "");
        ObjectStorageConfiguration storageConfig = new ObjectStorageConfiguration();
        storageConfig.setCloudProvider(CloudProvider.NONE);

        return DefaultJobDefinition.builder().jobClass(DatabaseChangeTask.class)
                .jobType(TaskType.ASYNC.name())
                .jobParameters(jobData)
                .build();
    }

}
