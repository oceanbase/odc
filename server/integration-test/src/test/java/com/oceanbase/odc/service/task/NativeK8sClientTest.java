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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.K8sJobResponse;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.test.database.TestProperties;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
@Ignore("manual test this case because k8s cluster is not public environment")
public class NativeK8sClientTest {

    private static K8sJobClient k8sClient;

    @BeforeClass
    public static void init() throws IOException {
        K8sProperties k8sProperties = new K8sProperties();
        k8sProperties.setKubeUrl(TestProperties.getProperty("odc.k8s.cluster.url"));
        k8sClient = new NativeK8sJobClient(k8sProperties);
    }

    @Test
    public void test_createJob() throws JobException {

        long exceptedTaskId = System.currentTimeMillis();
        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);

        String imageName = "perl:5.34.0";
        String exceptedJobName = JobUtils.generateExecutorName(jobIdentity);
        List<String> cmd = Arrays.asList("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)");
        PodConfig podParam = new PodConfig();
        String generateJobOfName = k8sClient.create("default", exceptedJobName, imageName, cmd, podParam);
        Assert.assertEquals(exceptedJobName, generateJobOfName);

        Optional<K8sJobResponse> queryJobName = k8sClient.get("default", exceptedJobName);
        Assert.assertTrue(queryJobName.isPresent());
        Assert.assertEquals(exceptedJobName, queryJobName.get());

        String deleteJobOfName = k8sClient.delete("default", exceptedJobName);
        Assert.assertEquals(exceptedJobName, deleteJobOfName);
    }

}
