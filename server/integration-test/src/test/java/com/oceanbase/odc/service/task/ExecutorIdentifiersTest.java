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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.K8sExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ProcessExecutorIdentifier;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;

/**
 * @author yaobin
 * @date 2023-12-25
 * @since 4.2.4
 */
public class ExecutorIdentifiersTest {


    @Test
    public void test_ProcessExecutorIdentifierToString() throws JobException {

        ProcessExecutorIdentifier pei = new ProcessExecutorIdentifier();
        pei.setIpv4Address("127.0.0.1");
        pei.setPid(1L);
        pei.setExecutorName("test-task-1");

        Assert.assertEquals("127.0.0.1;;test-task-1;1", pei.toString());

    }

    @Test
    public void test_K8sExecutorIdentifierToString() throws JobException {

        K8sExecutorIdentifier kei = new K8sExecutorIdentifier();
        kei.setRegion("cn-hangzhou");
        kei.setPodIdentity("iaas:cn-hangzhou:oceanbase:pod:p-UGd3iL");
        kei.setExecutorName("test-task-1");

        Assert.assertEquals(";cn-hangzhou;;;test-task-1;iaas:cn-hangzhou:oceanbase:pod:p-UGd3iL", kei.toString());

    }


    @Test
    public void test_StringToK8sExecutorIdentifier() throws JobException {

        String identifier = "aliyun;cn-hangzhou;;;test-task-1;iaas:cn-hangzhou:oceanbase:pod:p-UGd3iL";
        K8sExecutorIdentifier kei = (K8sExecutorIdentifier) ExecutorIdentifierParser
                .parser(TaskRunMode.K8S, identifier);

        Assert.assertEquals("aliyun", kei.getCloudProvider());
        Assert.assertEquals("cn-hangzhou", kei.getRegion());
        Assert.assertEquals("iaas:cn-hangzhou:oceanbase:pod:p-UGd3iL", kei.getPodIdentity());
        Assert.assertNull(kei.getNamespace());

    }

    @Test
    public void test_StringToProcessExecutorIdentifier() throws JobException {

        String identifier = "127.0.0.1;;test-task-1;1";
        ProcessExecutorIdentifier kei = (ProcessExecutorIdentifier) ExecutorIdentifierParser
                .parser(TaskRunMode.PROCESS, identifier);

        Assert.assertEquals("test-task-1", kei.getExecutorName());
        Assert.assertEquals("1", kei.getPid() + "");

    }



}
