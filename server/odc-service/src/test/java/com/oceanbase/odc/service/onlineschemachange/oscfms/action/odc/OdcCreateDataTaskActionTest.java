/*
 * Copyright (c) 2025 OceanBase.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsRequestUtil;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.sun.jna.platform.win32.OaIdl.DATE;

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
        resourceManager = Mockito.mock(ResourceManager.class);
        createDataTaskAction = new OdcCreateDataTaskAction(systemConfigService, resourceManager, onlineSchemaChangeProperties);
        podResource = new K8sPodResource("local", "local", "nativeK8s", "namespace",
            "arn", ResourceState.CREATING, null, null, null, new Date(System.currentTimeMillis() / 1000));
    }

    @Test
    public void testChooseUrlNotReadyWithNoIpReturned() {
        Assert.assertNull(createDataTaskAction.tryChooseUrl(podResource, -1));
    }

    @Test
    public void testChooseUrlNotReadyWithIpNotReached() {
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class);) {
            podResource.setPodIpAddress("127.0.0.1");
            commandUtil.when(() -> OscCommandUtil.isOSCMigrateSupervisorAlive(ArgumentMatchers.any())).thenReturn(false);
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

}
