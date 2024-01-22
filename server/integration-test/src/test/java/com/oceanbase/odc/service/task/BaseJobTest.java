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

import org.junit.BeforeClass;
import org.mockito.Mockito;

import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;
import com.oceanbase.odc.service.task.config.DefaultJobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.schedule.provider.DefaultHostUrlProvider;
import com.oceanbase.odc.service.task.schedule.provider.HostUrlProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.database.TestProperties;
import com.oceanbase.odc.test.util.JdbcUtil;

/**
 * @author yaobin
 * @date 2023-11-17
 * @since 4.2.4
 */
public abstract class BaseJobTest {

    protected static K8sJobClient k8sJobClient;
    protected static String imageName;
    protected static List<String> cmd;

    @BeforeClass
    public static void init() throws IOException {

        TestDBConfiguration tdc = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        System.setProperty(JobEnvKeyConstants.DATABASE_HOST, tdc.getHost());
        System.setProperty(JobEnvKeyConstants.DATABASE_PORT, tdc.getPort() + "");
        System.setProperty(JobEnvKeyConstants.DATABASE_NAME, tdc.getDefaultDBName());
        System.setProperty(JobEnvKeyConstants.DATABASE_USERNAME,
                JdbcUtil.buildUser(tdc.getUsername(), tdc.getTenant(), tdc.getCluster()));
        System.setProperty(JobEnvKeyConstants.DATABASE_PASSWORD, tdc.getPassword());
        System.setProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY, LogUtils.getBaseLogPath());
        System.setProperty(JobEnvKeyConstants.ODC_BOOT_MODE, JobConstants.ODC_BOOT_MODE_EXECUTOR);
        System.setProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE, TaskRunModeEnum.K8S.name());
        System.setProperty(JobEnvKeyConstants.ODC_SERVER_PORT, "8990");


        DefaultJobConfiguration jc = new DefaultJobConfiguration() {};

        HostProperties hostProperties = new HostProperties();
        hostProperties.setOdcHost("localhost");
        hostProperties.setPort("8990");
        HostUrlProvider urlProvider = new DefaultHostUrlProvider(
                () -> Mockito.mock(TaskFrameworkProperties.class), hostProperties);
        jc.setHostUrlProvider(urlProvider);
        jc.setTaskFrameworkService(Mockito.mock(TaskFrameworkService.class));
        JobConfigurationHolder.setJobConfiguration(jc);

        K8sProperties k8sProperties = new K8sProperties();
        k8sProperties.setKubeUrl(TestProperties.getProperty("odc.k8s.cluster.url"));
        k8sJobClient = new NativeK8sJobClient(k8sProperties);
        imageName = "perl:5.34.0";
        cmd = Arrays.asList("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)");
    }

    public static K8sJobClient getK8sJobClient() {
        return k8sJobClient;
    }

    public static String getImageName() {
        return imageName;
    }

    public static List<String> getCmd() {
        return cmd;
    }
}
