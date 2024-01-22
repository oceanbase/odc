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

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Slf4j
public class DestroyExecutorListener extends AbstractEventListener<DestroyExecutorEvent> {

    private final JobConfiguration jobConfiguration;

    public DestroyExecutorListener(JobConfiguration jobConfiguration) {
        this.jobConfiguration = jobConfiguration;
    }

    @Override
    public void onEvent(DestroyExecutorEvent event) {
        JobIdentity ji = event.getJi();
        JobEntity jobEntity = jobConfiguration.getTaskFrameworkService().find(ji.getId());

        if (jobEntity.getExecutorIdentifier() != null && jobEntity.getExecutorDestroyedTime() == null) {
            log.info("Accept job {} is finished by status {}, and try to destroy job.",
                    jobEntity.getId(), jobEntity.getStatus());
            try {
                jobConfiguration.getJobDispatcher().destroy(ji);
                log.warn("Destroy job {}, executor {} failed", jobEntity.getId(), jobEntity.getExecutorIdentifier());
            } catch (JobException e) {
                log.warn("Destroy job {}, executor {} failed, occur error: {}", jobEntity.getId(),
                        jobEntity.getExecutorIdentifier(), e.getMessage());
            }
        }

    }
}
