/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.agent.runtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.base.databasechange.DatabaseChangeTask;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentFactory;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-12-14
 * @since 4.2.4
 */
public class TaskApplicationTest {

    @Test
    public void test_executeDatabaseChangeTask_run() {
        Long exceptedTaskId = System.currentTimeMillis();
        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);

        setJobContextInSystemProperty(jobIdentity);
        startTaskApplication();
        assertRunningResult(jobIdentity);
    }


    private void setJobContextInSystemProperty(JobIdentity jobIdentity) {
        Map<String, String> envMap = buildConfig();
        JobUtils.encryptEnvironments(envMap);
        envMap.forEach(System::setProperty);
    }

    private void assertRunningResult(JobIdentity ji) {

        try {
            Thread.sleep(60 * 1000L);
            TaskRuntimeInfo taskRuntimeInfo = ThreadPoolTaskExecutor.getInstance().getTaskRuntimeInfo(ji);
            TaskContainer<?> task = taskRuntimeInfo.getTaskContainer();
            Assert.assertSame(JobStatus.DONE, task.getStatus());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private Map<String, String> buildConfig() {
        //validNotBlank(JobEnvKeyConstants.ODC_TASK_RUN_MODE);
        //        if (StringUtils.equalsIgnoreCase("PROCESS",
        //                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE))) {
        //            validNotBlank(JobEnvKeyConstants.ODC_JOB_CONTEXT_FILE_PATH);
        //        } else {
        //            validNotBlank(JobEnvKeyConstants.ODC_JOB_CONTEXT);
        //        }
        //        validNotBlank(JobEnvKeyConstants.ODC_BOOT_MODE);
        //        validNotBlank(JobEnvKeyConstants.ENCRYPT_SALT);
        //        validNotBlank(JobEnvKeyConstants.ENCRYPT_KEY);
        //        validNotBlank(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID);
        //        validNotBlank(JobEnvKeyConstants.ODC_LOG_DIRECTORY);

        Map<String, String> ret = new HashMap<>();
        ret.put(JobEnvKeyConstants.ODC_TASK_RUN_MODE, "PROCESS");
        ret.put(JobEnvKeyConstants.ODC_BOOT_MODE, "TASK_EXECUTOR");
        ret.put(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID, "1");
        ret.put(JobEnvKeyConstants.ODC_LOG_DIRECTORY, "log");
        return ret;
    }

    private void startTaskApplication() {
        new Thread(() -> new TaskApplication().run(null)).start();
    }

    private JobDefinition buildJobDefinition() {
        Long exceptedTaskId = System.currentTimeMillis();
        DatabaseChangeParameters parameters = new DatabaseChangeParameters();
        parameters.setSqlContent(String.format("CREATE TABLE %s (id int(10))", "t_" + exceptedTaskId));
        parameters.setErrorStrategy(TaskErrorStrategy.ABORT.name());
        PreConditions.validArgumentState(
                parameters.getSqlContent() != null || CollectionUtils.isNotEmpty(parameters.getSqlObjectIds()),
                ErrorCodes.BadArgument, new Object[] {"sql"}, "input sql is empty");

        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, JsonUtils.toJson(parameters));
        jobData.put(JobParametersKeyConstants.FLOW_INSTANCE_ID, exceptedTaskId + "");
        jobData.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS, 30 * 60 * 1000 + "");
        ObjectStorageConfiguration storageConfig = new ObjectStorageConfiguration();
        storageConfig.setCloudProvider(CloudProvider.NONE);

        return DefaultJobDefinition.builder().jobClass(DatabaseChangeTask.class)
                .jobType(TaskType.ASYNC.name())
                .jobParameters(jobData)
                .build();
    }
}
