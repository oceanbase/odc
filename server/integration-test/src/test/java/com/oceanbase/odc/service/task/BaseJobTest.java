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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.BeforeClass;
import org.mockito.Mockito;

import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;
import com.oceanbase.odc.service.task.config.DefaultJobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties.K8sProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.schedule.HostUrlProvider;
import com.oceanbase.odc.service.task.schedule.IpBasedHostUrlProvider;
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
    public static void init() throws IOException, URISyntaxException {
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);

        URL resource = BaseJobTest.class.getClassLoader().getResource("log4j2-task-executor.xml");
        // this will force a reconfiguration
        context.setConfigLocation(resource.toURI());

        TestDBConfiguration tdc = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        System.setProperty("DATABASE_HOST", tdc.getHost());
        System.setProperty("DATABASE_PORT", tdc.getPort() + "");
        System.setProperty("DATABASE_NAME", tdc.getDefaultDBName());
        System.setProperty("DATABASE_USERNAME",
                JdbcUtil.buildUser(tdc.getUsername(), tdc.getTenant(), tdc.getCluster()));
        System.setProperty("DATABASE_PASSWORD", tdc.getPassword());
        System.setProperty(JobEnvConstants.LOG_DIRECTORY, LogUtils.getBaseLogPath());
        System.setProperty(JobEnvConstants.BOOT_MODE, JobConstants.ODC_BOOT_MODE_EXECUTOR);
        System.setProperty(JobEnvConstants.TASK_RUN_MODE, TaskRunModeEnum.K8S.name());
        DefaultJobConfiguration jc = new DefaultJobConfiguration() {};

        HostUrlProvider urlProvider = new IpBasedHostUrlProvider(new HostProperties());
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
