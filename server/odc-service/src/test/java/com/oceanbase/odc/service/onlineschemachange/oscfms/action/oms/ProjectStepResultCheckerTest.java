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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;

/**
 * @author longpeng.zlp
 * @date 2024/8/2 11:35
 * @since 4.3.1
 */
public class ProjectStepResultCheckerTest {
    private List<OmsProjectStepVO> projectSteps;

    @Before
    public void init() {
        projectSteps = new ArrayList<>();
        OmsProjectStepVO incrementStep = new OmsProjectStepVO();
        incrementStep.setStatus(OmsStepStatus.MONITORING);
        incrementStep.setProgress(100);
        incrementStep.setName(OscStepName.INCR_TRANSFER);
        projectSteps.add(incrementStep);
    }

    @Test
    public void testDataApplyTaskReady() {
        OmsProjectProgressResponse response = new OmsProjectProgressResponse();
        response.setIncrSyncCheckpoint(System.currentTimeMillis() / 1000 - 10);
        ProjectStepResultChecker checker = new ProjectStepResultChecker(response, projectSteps,
                false, 1, Collections.emptyMap());
        Assert.assertTrue(checker.checkStepFinished(OscStepName.INCR_TRANSFER));
    }

    @Test
    public void testDataApplyTaskNotReady() {
        OmsProjectProgressResponse response = new OmsProjectProgressResponse();
        response.setIncrSyncCheckpoint(System.currentTimeMillis() / 1000 - 50);
        ProjectStepResultChecker checker = new ProjectStepResultChecker(response, projectSteps,
                false, 1, Collections.emptyMap());
        Assert.assertFalse(checker.checkStepFinished(OscStepName.INCR_TRANSFER));
    }
}
