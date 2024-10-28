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
package com.oceanbase.odc.service.task.status;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.state.JobStatusFsm;

/**
 * @author longpeng.zlp
 * @date 2024/10/23 09:54
 */
public class JobStatusFsmTest {
    private final JobStatusFsm jobStatusFsm = new JobStatusFsm();

    @Test
    public void testJobStatusFsm() {
        // check remaining
        checkStatusRemain(TaskStatus.RUNNING);
        checkStatusRemain(TaskStatus.PREPARING);
        // check transfer
        checkStatusTransfer(TaskStatus.CANCELED, JobStatus.CANCELED);
        checkStatusTransfer(TaskStatus.DONE, JobStatus.DONE);
        checkStatusTransfer(TaskStatus.FAILED, JobStatus.FAILED);
        checkStatusTransfer(TaskStatus.ABNORMAL, JobStatus.FAILED);
    }

    private void checkStatusRemain(TaskStatus remainStatus) {
        for (JobStatus jobStatus : JobStatus.values()) {
            Assert.assertEquals(jobStatus, jobStatusFsm.determinateJobStatus(jobStatus, remainStatus));
        }
    }

    private void checkStatusTransfer(TaskStatus remainStatus, JobStatus targetStatus) {
        for (JobStatus jobStatus : JobStatus.values()) {
            Assert.assertEquals(targetStatus, jobStatusFsm.determinateJobStatus(jobStatus, remainStatus));
        }
    }
}
