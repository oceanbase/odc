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
package com.oceanbase.odc.service.lab;

import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.config.EncryptableConfigurations;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.CreateUserReq;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.lab.model.LabProperties;

public class ResourceServiceTest extends ServiceTestEnv {

    private static final String TEST_CONFIG_FILE = "src/test/resources/lab-test.properties";
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private LabDataSourceFactory labDataSourceFactory;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TestLabProperties testLabProperties;
    @MockBean
    private LabProperties labProperties;
    @MockBean
    private ProjectService projectService;
    @MockBean
    private EnvironmentService environmentService;

    private User user;

    @Before
    public void setUp() {
        CreateUserReq createUserReq = new CreateUserReq();
        createUserReq.setName("test");
        createUserReq.setAccountName("test");
        createUserReq.setPassword("test");
        createUserReq.setEnabled(true);
        this.user = userService.create(createUserReq);

        when(labProperties.isLabEnabled()).thenReturn(true);
        when(labProperties.getInitMysqlResourceInitScriptTemplate())
                .thenReturn(testLabProperties.getInitMysqlResourceInitScriptTemplate());
        when(labProperties.getInitMysqlResourceRevokeScriptTemplate())
                .thenReturn(testLabProperties.getInitMysqlResourceRevokeScriptTemplate());
        when(labProperties.getObConnectionKey()).thenReturn(
                JsonUtils.toJson(Arrays.asList(EncryptableConfigurations.loadProperties(TEST_CONFIG_FILE))));
        when(environmentService.detailSkipPermissionCheck(Mockito.anyLong())).thenReturn(getEnvironment());
        when(environmentService.exists(Mockito.anyLong())).thenReturn(true);


        labDataSourceFactory.init();
    }

    @After
    public void tearDown() throws Exception {
        if (this.user != null) {
            userService.delete(this.user.getId());
        }
    }

    @Test
    @Ignore
    public void createThenRevoke() {
        resourceService.createResource(this.user);

        int connectionCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
                "connect_connection", "`name` like 'oceanbase_mysql_test_%'");
        Assert.assertEquals(1, connectionCount);

        Long connectionId = jdbcTemplate.queryForObject(
                "select `id` from connect_connection where `name` like 'oceanbase_mysql_test_%'",
                Long.class);

        boolean ret = resourceService.revokeResource(connectionId);
        Assert.assertTrue(ret);
    }

    private Environment getEnvironment() {
        Environment environment = new Environment();
        environment.setId(1L);
        environment.setName("fake_env");
        return environment;
    }

}
