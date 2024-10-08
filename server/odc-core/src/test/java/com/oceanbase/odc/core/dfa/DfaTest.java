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
package com.oceanbase.odc.core.dfa;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link AbstractDfa}
 *
 * @author yh263208
 * @date 2024-09-04 21:44
 * @since ODC_release_4.3.2
 */
public class DfaTest {

    private static final String FIND_MACHINE = "Find Machine";
    private static final String POD_CREATED_SUCCEED = "Pod Created Succeed";
    private static final String IMG_PULL_ERR = "Image pull error";
    private static final String POD_DELETE = "Pod is deleting";
    private static final String POD_DELETED = "Pod is deleted";

    @Test
    public void next_stateExistsRightEvent_nextStateSucceed() throws Exception {
        AbstractDfa<String, String> fsm = buildFsm();
        fsm.setCurrentState("Pending");
        fsm.next(FIND_MACHINE);
        Assert.assertEquals("Creating", fsm.getCurrentState());
    }

    @Test(expected = IllegalStateException.class)
    public void next_illegalState_expThrown() throws Exception {
        AbstractDfa<String, String> fsm = buildFsm();
        fsm.setCurrentState("Abc");
        fsm.next(FIND_MACHINE);
    }

    @Test
    public void next_reachFinalState_expThrown() throws Exception {
        AbstractDfa<String, String> fsm = buildFsm();
        fsm.setCurrentState("Pending");
        fsm.next(FIND_MACHINE)
                .next(POD_CREATED_SUCCEED)
                .next(POD_DELETE)
                .next(POD_DELETED);
    }

    private AbstractDfa<String, String> buildFsm() {
        return new K8sPodStatusDfa(Arrays.asList(
                new K8sPodStatusDfaTransfer("Pending", "Creating", FIND_MACHINE),
                new K8sPodStatusDfaTransfer("Creating", "Running", POD_CREATED_SUCCEED),
                new K8sPodStatusDfaTransfer("Creating", "ImgBackOff", IMG_PULL_ERR),
                new K8sPodStatusDfaTransfer("Running", "Deleting", POD_DELETE),
                new K8sPodStatusDfaTransfer("Deleting", "Deleted", POD_DELETED)));
    }

}
