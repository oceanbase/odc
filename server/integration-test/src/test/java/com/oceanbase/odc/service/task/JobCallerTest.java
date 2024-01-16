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

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.K8sJobCaller;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTaskJobDefinitionBuilder;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-17
 * @since 4.2.4
 */
@Slf4j
@Ignore("manual test this case because k8s cluster is not public environment")
public class JobCallerTest extends BaseJobTest {

    @Test
    public void test_startJob() throws JobException {
        Long exceptedTaskId = System.currentTimeMillis();
        PodConfig podConfig = new PodConfig();
        podConfig.setImage(getImageName());
        podConfig.setCommand(getCmd());
        podConfig.setNamespace("default");

        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        JobCaller jobCaller = new K8sJobCaller(getK8sJobClient(), podConfig);

        DefaultJobContext context = new DefaultJobContext();
        context.setJobIdentity(jobIdentity);
        jobCaller.start(context);
        context.setJobIdentity(jobIdentity);
        jobCaller.stop(JobIdentity.of(exceptedTaskId));
    }


    @Test
    public void test_startSampleTask() throws JobException, InterruptedException {
        Long exceptedTaskId = System.currentTimeMillis();
        PodConfig podConfig = new PodConfig();
        podConfig.setImage("mengdezhicai/odc:test-task-latest");
        podConfig.getPodParam().setImagePullPolicy(JobConstants.IMAGE_PULL_POLICY_ALWAYS);
        podConfig.setNamespace("default");

        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        JobCaller jobCaller = JobCallerBuilder.buildK8sJobCaller(getK8sJobClient(), podConfig);

        String sql1 = String.format("CREATE TABLE %s (id int(10))", "t_" + exceptedTaskId);
        List<String> sqls = new ArrayList<>();
        sqls.add(sql1);

        ConnectionConfig connectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);

        JobDefinition jd = new SampleTaskJobDefinitionBuilder()
                .build(connectionConfig, connectionConfig.getDefaultSchema(), sqls);
        JobContext jc = new DefaultJobContextBuilder().build(jobIdentity, jd);

        jobCaller.start(jc);
        Thread.sleep(60000);
        JobIdentity newJi = JobIdentity.of(exceptedTaskId);
        jobCaller.stop(newJi);
    }

}
