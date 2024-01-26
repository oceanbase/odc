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

import org.junit.BeforeClass;
import org.mockito.Mockito;

import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.config.DefaultJobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.schedule.provider.DefaultHostUrlProvider;
import com.oceanbase.odc.service.task.schedule.provider.HostUrlProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.util.JdbcUtil;

/**
 * @author yaobin
 * @date 2023-11-17
 * @since 4.2.4
 */
public abstract class BaseJobTest {


    @BeforeClass
    public static void init() throws IOException {

        TestDBConfiguration tdc = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        System.setProperty(JobEnvKeyConstants.ODC_DATABASE_HOST, tdc.getHost());
        System.setProperty(JobEnvKeyConstants.ODC_DATABASE_PORT, tdc.getPort() + "");
        System.setProperty(JobEnvKeyConstants.ODC_DATABASE_NAME, tdc.getDefaultDBName());
        System.setProperty(JobEnvKeyConstants.ODC_DATABASE_USERNAME,
                JdbcUtil.buildUser(tdc.getUsername(), tdc.getTenant(), tdc.getCluster()));
        System.setProperty(JobEnvKeyConstants.ODC_DATABASE_PASSWORD, tdc.getPassword());
        String port = "8990";
        System.setProperty(JobEnvKeyConstants.ODC_SERVER_PORT, port);
        System.setProperty(JobEnvKeyConstants.REPORT_ENABLED, Boolean.FALSE.toString());

        DefaultJobConfiguration jc = new DefaultJobConfiguration() {};

        HostProperties hostProperties = new HostProperties();
        hostProperties.setOdcHost("localhost");
        hostProperties.setPort(port);
        HostUrlProvider urlProvider = new DefaultHostUrlProvider(
                () -> Mockito.mock(TaskFrameworkProperties.class), hostProperties);
        jc.setHostUrlProvider(urlProvider);
        jc.setTaskFrameworkService(Mockito.mock(TaskFrameworkService.class));
        JobConfigurationHolder.setJobConfiguration(jc);



    }
}
