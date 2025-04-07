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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.RateLimiterConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ConnectionProvider;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsRequestUtil;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.K8sResourceUtil;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.resource.K8sPodResource;

/**
 * @author longpeng.zlp
 * @date 2025/4/2 16:45
 */
public class OdcCreateDataTaskActionTest {
    private OdcCreateDataTaskAction createDataTaskAction;
    private SystemConfigService systemConfigService;
    private ResourceManager resourceManager;
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    private K8sPodResource podResource;
    private ConnectionConfig connectionConfig;
    private ScheduleTaskRepository repository;
    private OnlineSchemaChangeScheduleTaskParameters parameters;
    private OscActionContext context;
    private ScheduleTaskEntity scheduleTask;

    @Before
    public void init() {
        onlineSchemaChangeProperties = new OnlineSchemaChangeProperties();
        onlineSchemaChangeProperties.setEnableFullVerify(false);
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1:8089");
        omsProperties.setRegion("default");
        omsProperties.setAuthorization("auth");
        onlineSchemaChangeProperties.setOms(omsProperties);
        systemConfigService = Mockito.mock(SystemConfigService.class);
        List<Configuration> kubeConfigUrls =
                Arrays.asList(new Configuration("odc.task-framework.k8s-properties.kube-config", "xxx"));
        Mockito.when(systemConfigService.queryByKeyPrefix("odc.task-framework.k8s-properties.")).thenReturn(
                kubeConfigUrls);
        List<Configuration> oscConfigUrls =
                Arrays.asList(new Configuration("odc.osc.k8s-properties.pod-pending-timeout-seconds", "600"),
                        new Configuration("odc.osc.k8s-properties.namespace", "namespace"),
                        new Configuration("odc.osc.k8s-properties.pod-image-name", "imageName"));
        Mockito.when(systemConfigService.queryByKeyPrefix("odc.osc.k8s-properties.")).thenReturn(
                oscConfigUrls);

        Mockito.when(systemConfigService.queryByKeyPrefix("odc.task-framework.k8s-properties.")).thenReturn(
                kubeConfigUrls);
        resourceManager = Mockito.mock(ResourceManager.class);
        createDataTaskAction =
                new OdcCreateDataTaskAction(systemConfigService, resourceManager, onlineSchemaChangeProperties);
        podResource = new K8sPodResource("local", "local", "nativeK8s", "namespace",
                "arn", ResourceState.CREATING, null, null, null, new Date(System.currentTimeMillis() / 1000));
        connectionConfig = new ConnectionConfig();
        connectionConfig.setRegion("region");
        connectionConfig.setAttributes(Collections.singletonMap("cloudProvider", "ll"));
        connectionConfig.setUsername("user");
        connectionConfig.setPassword("pwd");
        connectionConfig.setTenantName("tenant");
        connectionConfig.setClusterName("cluster");
        repository = Mockito.mock(ScheduleTaskRepository.class);
        Mockito.when(repository.updateTaskParameters(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(1);
        parameters = new OnlineSchemaChangeScheduleTaskParameters();
        parameters.setResourceID(1L);
        parameters.setDatabaseName("dbName");
        parameters.setOriginTableName("srcTable");
        parameters.setNewTableName("ghost_srcTable");
        parameters.setFilterColumns(Arrays.asList("c1", "c2"));
        RateLimiterConfig config = new RateLimiterConfig();
        config.setRowLimit(1024);
        parameters.setRateLimitConfig(config);
        context = new OscActionContext();
        scheduleTask = new ScheduleTaskEntity();
        scheduleTask.setId(2L);
        ConnectionProvider connectionProvider = new ConnectionProvider() {
            @Override
            public ConnectionConfig connectionConfig() {
                return connectionConfig;
            }

            @Override
            public ConnectionSession createConnectionSession() {
                return Mockito.mock(ConnectionSession.class);
            }
        };
        context.setScheduleTaskRepository(repository);
        context.setTaskParameter(parameters);
        context.setScheduleTask(scheduleTask);
        context.setConnectionProvider(connectionProvider);
    }

    @Test
    public void testChooseUrlNotReadyWithNoIpReturned() {
        Assert.assertNull(createDataTaskAction.tryChooseUrl(podResource, -1));
    }

    @Test
    public void testChooseUrlNotReadyWithIpNotReached() {
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class);) {
            podResource.setPodIpAddress("127.0.0.1");
            commandUtil.when(() -> OscCommandUtil.isOSCMigrateSupervisorAlive(ArgumentMatchers.any()))
                    .thenReturn(false);
            Assert.assertNull(createDataTaskAction.tryChooseUrl(podResource, -1));
            podResource.setHostIpAddress("127.0.0.1");
            Assert.assertNull(createDataTaskAction.tryChooseUrl(podResource, -1));
            Assert.assertNull(createDataTaskAction.tryChooseUrl(podResource, 100));
        }
    }

    @Test
    public void testChooseUrlReady() {
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class);) {
            podResource.setPodIpAddress("127.0.0.1");
            commandUtil.when(() -> OscCommandUtil.isOSCMigrateSupervisorAlive(ArgumentMatchers.any())).thenReturn(true);
            Assert.assertEquals(createDataTaskAction.tryChooseUrl(podResource, -1), "http://127.0.0.1:18001");
            podResource.setPodIpAddress(null);
            podResource.setHostIpAddress("127.0.0.1");
            Assert.assertEquals(createDataTaskAction.tryChooseUrl(podResource, 100), "http://127.0.0.1:100");
        }
    }

    @Test
    public void testBuildK8sProperties() {
        K8sProperties k8sProperties = createDataTaskAction.buildK8sProperties(connectionConfig);
        Assert.assertEquals(k8sProperties.getKubeConfig(), "xxx");
        Assert.assertEquals(k8sProperties.getNamespace(), "namespace");
        Assert.assertEquals(k8sProperties.getPodImageName(), "imageName");
        Assert.assertEquals(k8sProperties.getPodPendingTimeoutSeconds().longValue(), 600L);
        Assert.assertNull(k8sProperties.getExecutorListenPort());
        Assert.assertEquals(k8sProperties.getSupervisorListenPort().intValue(), 18001);
        Assert.assertEquals(k8sProperties.getNodeMemInMB().longValue(), 16384);
    }

    @Test
    public void testWaitMigrateNodeNotReady() throws Exception {
        podResource.setPodIpAddress("host");
        podResource.setHostIpAddress("host");
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class);
                MockedStatic<K8sResourceUtil> k8sUtil = Mockito.mockStatic(K8sResourceUtil.class)) {
            k8sUtil.when(() -> K8sResourceUtil.queryIpAndAddress(ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
                    .thenReturn(podResource);
            commandUtil.when(() -> OscCommandUtil.isOSCMigrateSupervisorAlive(ArgumentMatchers.any()))
                    .thenReturn(false);
            Assert.assertFalse(createDataTaskAction.waitMigrateNodeReady(context, parameters));
        }
    }

    @Test
    public void testWaitMigrateNodeReady() throws Exception {
        podResource.setPodIpAddress("host1");
        podResource.setHostIpAddress("host2");
        parameters.setK8sMapperPort(1024);
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class);
                MockedStatic<K8sResourceUtil> k8sUtil = Mockito.mockStatic(K8sResourceUtil.class)) {
            k8sUtil.when(() -> K8sResourceUtil.queryIpAndAddress(ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
                    .thenReturn(podResource);
            commandUtil.when(() -> OscCommandUtil.isOSCMigrateSupervisorAlive("http://host1:18001")).thenReturn(false);
            commandUtil.when(() -> OscCommandUtil.isOSCMigrateSupervisorAlive("http://host2:1024")).thenReturn(true);
            Assert.assertTrue(createDataTaskAction.waitMigrateNodeReady(context, parameters));
            Assert.assertEquals("http://host2:1024", parameters.getOdcCommandURl());
        }
    }

    @Test
    public void testStartTask() throws Exception {
        CreateOceanBaseDataSourceRequest dataSourceRequest = new CreateOceanBaseDataSourceRequest();
        String ip = "ip";
        Integer port = 123;
        String configUrl = "configUrl";
        String drcUser = "drcUser";
        String drcPwd = "drcPwd";
        String drcCluster = "cluster";
        dataSourceRequest.setIp(ip);
        dataSourceRequest.setPort(port);
        dataSourceRequest.setPassword("pwd");
        dataSourceRequest.setConfigUrl(configUrl);
        dataSourceRequest.setDrcUserName(drcUser);
        dataSourceRequest.setDrcPassword(drcPwd);
        dataSourceRequest.setCluster(drcCluster);

        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class);
                MockedStatic<OmsRequestUtil> omsRequestUtil = Mockito.mockStatic(OmsRequestUtil.class)) {
            commandUtil.when(() -> OscCommandUtil.startTask(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenReturn(new SupervisorResponse(true, null, null));
            omsRequestUtil.when(() -> OmsRequestUtil.getCreateDataSourceRequest(ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.any())).thenReturn(dataSourceRequest);
            createDataTaskAction.startTask(context, parameters);
            ArgumentCaptor<Map<String, String>> startParametersCapture = ArgumentCaptor.forClass(Map.class);
            commandUtil
                    .verify(() -> OscCommandUtil.startTask(ArgumentMatchers.any(), startParametersCapture.capture()));
            Map<String, String> startParameters = startParametersCapture.getValue();
            // fill datasource
            Assert.assertEquals(startParameters.get("databaseUrl"), "ip:123");
            Assert.assertEquals(startParameters.get("databaseUser"), "user@tenant#cluster");
            Assert.assertEquals(startParameters.get("databasePassword"), "pwd");
            Assert.assertEquals(startParameters.get("crawlerClusterURL"), "configUrl");
            Assert.assertEquals(startParameters.get("crawlerClusterUser"), "drcUser");
            Assert.assertEquals(startParameters.get("crawlerClusterPassword"), "drcPwd");
            Assert.assertEquals(startParameters.get("crawlerClusterAppName"), "cluster");
            // fill osc table configs
            Assert.assertEquals(startParameters.get("dbname"), "dbName");
            Assert.assertEquals(startParameters.get("tenantName"), "tenant");
            Assert.assertEquals(startParameters.get("sourceTableName"), "srcTable");
            Assert.assertEquals(startParameters.get("targetTableName"), "ghost_srcTable");
            Assert.assertEquals(startParameters.get("targetToSrcColMapper"), "{\"c1\":\"c1\",\"c2\":\"c2\"}");
            Assert.assertEquals(startParameters.get("throttleRps"), "1024");
        }
    }
}
