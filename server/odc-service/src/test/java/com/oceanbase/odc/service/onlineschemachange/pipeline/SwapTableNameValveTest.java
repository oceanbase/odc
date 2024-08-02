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
package com.oceanbase.odc.service.onlineschemachange.pipeline;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.oms.OmsRequestUtil;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.pipeline.ProjectStepResultChecker.ProjectStepResult;

/**
 * @author longpeng.zlp
 * @date 2024/8/2 11:02
 * @since 4.3.1
 */
public class SwapTableNameValveTest {
    private OmsProjectOpenApiService projectOpenApiService;
    private OnlineSchemaChangeProperties properties;

    @Before
    public void init() {
        projectOpenApiService = Mockito.mock(OmsProjectOpenApiService.class);
        properties = new OnlineSchemaChangeProperties();
    }

    @Test
    public void testSwapTableReady() {
        // MockStatic instance should closed after use
        try (MockedStatic<OmsRequestUtil> requestUtil = Mockito.mockStatic(OmsRequestUtil.class);) {
            ProjectStepResult projectStepResult = new ProjectStepResult();
            // valid checkpoint
            long checkpoint = System.currentTimeMillis() / 1000 + 10;
            projectStepResult.setIncrementCheckpoint(checkpoint);
            requestUtil.when(() -> OmsRequestUtil.buildProjectStepResult(ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.any())).thenReturn(projectStepResult);
            requestUtil.when(() -> OmsRequestUtil.OMSTaskReady(projectStepResult)).thenReturn(true);
            SwapTableNameValve swapTableNameValve = new SwapTableNameValve();
            Assert.assertTrue(swapTableNameValve.isIncrementDataAppliedDone(projectOpenApiService, properties,
                    "uid", "projectID", "db", Collections.emptyMap(), 1000));
        }
    }

    @Test
    public void testSwapTableNotReady() {
        try (MockedStatic<OmsRequestUtil> requestUtil = Mockito.mockStatic(OmsRequestUtil.class);) {
            ProjectStepResult projectStepResult = new ProjectStepResult();
            // valid checkpoint
            long checkpoint = System.currentTimeMillis() / 1000 - 10;
            projectStepResult.setIncrementCheckpoint(checkpoint);
            requestUtil.when(() -> OmsRequestUtil.buildProjectStepResult(ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyString(),
                    ArgumentMatchers.any())).thenReturn(projectStepResult);
            requestUtil.when(() -> OmsRequestUtil.OMSTaskReady(projectStepResult)).thenReturn(true);
            SwapTableNameValve swapTableNameValve = new SwapTableNameValve();
            long currentTimeMS = System.currentTimeMillis();
            Assert.assertFalse(swapTableNameValve.isIncrementDataAppliedDone(projectOpenApiService, properties,
                    "uid", "projectID", "db", Collections.emptyMap(), 3000));
            long endTimeMS = System.currentTimeMillis();
            // test retry
            Assert.assertTrue(endTimeMS - currentTimeMS > 2000);
        }
    }
}
