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

package com.oceanbase.odc.service.k8s;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.caller.PodParam;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
@Ignore("manual test this case because k8s cluster is not public environment")
public class PrimitiveJobBasedK8sClientTest extends BaseJobTest {

    @Test
    public void test_createJob() throws JobException {

        long taskId = 1L;
        String exceptedJobName = JobUtils.generateJobName(taskId);
        PodParam podParam = new PodParam();
        podParam.setTtlSecondsAfterFinished(3);
        String generateJobOfName = k8sClient.createNamespaceJob("default", exceptedJobName,
                getImageName(), getCmd(), podParam);
        Assert.assertEquals(exceptedJobName, generateJobOfName);

        Optional<String> queryJobName = k8sClient.getNamespaceJob("default", exceptedJobName);
        Assert.assertTrue(queryJobName.isPresent());
        Assert.assertEquals(exceptedJobName, queryJobName.get());
    }

}
