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
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.executor.ExitHelper;
import com.oceanbase.odc.service.task.executor.TaskApplication;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.SampleTaskJobDefinitionBuilder;

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
        new Thread(() -> {
            try {
                Thread.sleep(20 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                ExitHelper.exit();
            }
        }).start();
        new TaskApplication().run(null);
    }


    @Test
    public void test_executeDatabaseChangeTask() {
        Long exceptedTaskId = System.currentTimeMillis();;
        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        ConnectionConfig connectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        List<String> sqls =
            Collections.singletonList(String.format("CREATE TABLE %s (id int(10))", "t_" + exceptedTaskId));

        JobDefinition jd = new SampleTaskJobDefinitionBuilder()
            .build(connectionConfig, connectionConfig.getDefaultSchema(), sqls);
        JobContext jc = new DefaultJobContextBuilder().build(jobIdentity, jd);
        System.setProperty(JobEnvConstants.TASK_ALL_PARAMETERS, JobUtils.toJson(jc));
        new Thread(() -> {
            try {
                Thread.sleep(20 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                ExitHelper.exit();
            }
        }).start();
        new TaskApplication().run(null);
    }
}
