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

package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;

/**
 * @author longpeng.zlp
 * @date 2025/4/2 16:30
 */
public class OmsCleanResourcesActionTest {
    private OmsProjectOpenApiService omsProjectOpenApiService;


    @Before
    public void init() {
        omsProjectOpenApiService = Mockito.mock(OmsProjectOpenApiService.class);
        Mockito.when(omsProjectOpenApiService.createProject(ArgumentMatchers.any())).thenReturn("testProjectID");
    }


    @Test
    public void testCheckAndReleaseProjectRunning() {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RUNNING);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        // releaseProject should be called
        Assert.assertFalse(omsCleanResourcesAction.checkAndReleaseProject("omsProjectID", "uid"));
        Mockito.verify(omsProjectOpenApiService, Mockito.times(1)).releaseProject(ArgumentMatchers.any());
    }

    @Test
    public void testCheckAndReleaseProjectDone() {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RELEASED);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        // releaseProject should be called
        Assert.assertTrue(omsCleanResourcesAction.checkAndReleaseProject("omsProjectID", "uid"));
        Mockito.verify(omsProjectOpenApiService, Mockito.times(0)).releaseProject(ArgumentMatchers.any());
    }
}
