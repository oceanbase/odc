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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.executor.ExitHelper;
import com.oceanbase.odc.service.task.executor.TaskApplication;
import com.oceanbase.odc.service.task.executor.task.DatabaseChangeTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.SampleTaskJobDefinitionBuilder;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-12-14
 * @since 4.2.4
 */
@Ignore("manual run this case")
public class TaskApplicationTest extends BaseJobTest {

    @Test
    public void test_executeSimpleTask() {
        Long exceptedTaskId = System.currentTimeMillis();;
        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        ConnectionConfig connectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        List<String> sqls =
                Collections.singletonList(String.format("CREATE TABLE %s (id int(10))", "t_" + exceptedTaskId));

        JobDefinition jd = new SampleTaskJobDefinitionBuilder()
                .build(connectionConfig, connectionConfig.getDefaultSchema(), sqls);
        JobContext jc = new DefaultJobContextBuilder().build(jobIdentity, jd);
        System.setProperty(JobEnvConstants.TASK_ALL_PARAMETERS, JobUtils.toJson(jc));
        mainMethodExit(20 * 1000);
        new TaskApplication().run(null);
    }


    @Test
    public void test_executeDatabaseChangeTask() {
        Long exceptedTaskId = System.currentTimeMillis();
        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);

        JobDefinition jd = buildJobDefinition();
        JobContext jc = new DefaultJobContextBuilder().build(jobIdentity, jd);
        System.setProperty(JobEnvConstants.TASK_ALL_PARAMETERS, JobUtils.toJson(jc));
        mainMethodExit(60 * 1000);
        new TaskApplication().run(null);
    }

    private void mainMethodExit(int waitMills) {
        new Thread(() -> {
            try {
                Thread.sleep(waitMills);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                ExitHelper.exit();
            }
        }).start();
    }

    private JobDefinition buildJobDefinition() {
        Long exceptedTaskId = System.currentTimeMillis();
        DatabaseChangeParameters parameters = new DatabaseChangeParameters();
        parameters.setSqlContent(String.format("CREATE TABLE %s (id int(10))", "t_" + exceptedTaskId));
        parameters.setErrorStrategy(TaskErrorStrategy.ABORT.name());
        PreConditions.validArgumentState(
                parameters.getSqlContent() != null || CollectionUtils.isNotEmpty(parameters.getSqlObjectIds()),
                ErrorCodes.BadArgument, new Object[] {"sql"}, "input sql is empty");

        ConnectionConfig config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobDataMapConstants.META_DB_TASK_PARAMETER, JsonUtils.toJson(parameters));
        jobData.put(JobDataMapConstants.CONNECTION_CONFIG, JobUtils.toJson(config));
        jobData.put(JobDataMapConstants.FLOW_INSTANCE_ID, exceptedTaskId + "");
        jobData.put(JobDataMapConstants.CURRENT_SCHEMA_KEY, config.getDefaultSchema());
        jobData.put(JobDataMapConstants.TIMEOUT_MILLI_SECONDS, 30 * 60 * 1000 + "");
        ObjectStorageConfiguration storageConfig = new ObjectStorageConfiguration();
        storageConfig.setCloudProvider(CloudProvider.NONE);
        jobData.put(JobDataMapConstants.OBJECT_STORAGE_CONFIGURATION, JsonUtils.toJson(storageConfig));

        return DefaultJobDefinition.builder().jobClass(DatabaseChangeTask.class)
                .jobType(TaskType.ASYNC.name())
                .jobParameters(jobData)
                .build();
    }
}
