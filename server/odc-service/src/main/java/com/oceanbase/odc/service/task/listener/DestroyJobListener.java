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

package com.oceanbase.odc.service.task.listener;

import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Slf4j
public class DestroyJobListener extends TaskResultUploadListener {

    private final JobScheduler jobScheduler;

    public DestroyJobListener(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @Override
    public void onEvent(TaskResultUploadEvent event) {
        /*
         * if (event.getTaskResult().getTaskStatus() == TaskStatus.DESTROYED) { try {
         * jobScheduler.cancelJob(event.getTaskResult().getJobIdentity().getId()); } catch (JobException e)
         * { log.warn("Cancel job failed"); } }
         */
    }
}
