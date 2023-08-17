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
package com.oceanbase.odc.service.schedule;

import java.util.Optional;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskResp;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/30 19:07
 * @Descripition:
 */

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class ScheduleTaskService {

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    private QuartzJobService quartzJobService;

    public ScheduleTaskEntity create(ScheduleTaskEntity taskEntity) {
        return scheduleTaskRepository.save(taskEntity);
    }

    /**
     * Trigger an existing task to retry or resume a terminated task.
     */
    public ScheduleTaskResp start(Long taskId) {
        ScheduleTaskEntity taskEntity = nullSafeGetById(taskId);
        if (taskEntity.getStatus() == TaskStatus.RUNNING || taskEntity.getStatus() == TaskStatus.DONE) {
            throw new IllegalStateException();
        }
        try {
            quartzJobService.triggerJob(new JobKey(taskEntity.getJobName(), taskEntity.getJobGroup()),
                    ScheduleTaskUtils.buildTriggerDataMap(taskId));
        } catch (SchedulerException e) {
            log.warn("Start task failed,taskId={}", taskId);
            throw new InternalServerError(e.getMessage());
        }
        return ScheduleTaskResp.withId(taskId);
    }



    public Page<ScheduleTaskEntity> listTask(Pageable pageable, Long scheduleId) {
        Specification<ScheduleTaskEntity> specification =
                Specification.where(ScheduleTaskSpecs.jobNameEquals(scheduleId.toString()));
        return scheduleTaskRepository.findAll(specification, pageable);
    }


    public ScheduleTaskEntity nullSafeGetById(Long id) {
        Optional<ScheduleTaskEntity> scheduleEntityOptional = scheduleTaskRepository.findById(id);
        return scheduleEntityOptional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE_TASK, "id", id));
    }
}
