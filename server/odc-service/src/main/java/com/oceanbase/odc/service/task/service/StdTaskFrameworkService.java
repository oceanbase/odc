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

package com.oceanbase.odc.service.task.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-30
 * @since 4.2.4
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class StdTaskFrameworkService implements TaskFrameworkService {

    @Autowired
    private JobScheduler jobScheduler;
    @Autowired
    private JobScheduleRepository jobScheduleRepository;

    @Autowired(required = false)
    private List<ResultHandleService> resultHandleServices;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(TaskResult taskResult) {
        if (taskResult.getTaskStatus().isTerminated()) {
            log.warn("task is terminated, ignore upload result.{}", JsonUtils.toJson(taskResult));
            return;
        }
        if (resultHandleServices != null) {
            resultHandleServices.forEach(r -> r.handle(taskResult));
        }
        updateJobScheduleEntity(taskResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobEntity save(JobDefinition jd) {
        JobEntity jse = new JobEntity();
        jse.setJobDataJson(JsonUtils.toJson(jd.getJobData()));
        jse.setScheduleTimes(0);
        jse.setJobClass(jd.getJobClass().getCanonicalName());
        jse.setJobType(jd.getJobType());
        jse.setStatus(TaskStatus.PREPARING);
        return jobScheduleRepository.save(jse);
    }


    @Override
    public JobEntity find(JobIdentity ji) {
        return jobScheduleRepository.findById(ji.getId())
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", ji.getId()));
    }


    private void updateJobScheduleEntity(TaskResult taskResult) {
        JobEntity jse = find(taskResult.getJobIdentity());
        updateStatusAndScheduleTimes(jse.getId(), taskResult.getTaskStatus(), jse.getScheduleTimes() + 1);
    }


    private void updateStatusAndScheduleTimes(Long id, TaskStatus status, Integer scheduleTimes) {
        jobScheduleRepository.updateStatusAndScheduleTimesById(id, status, scheduleTimes);
    }

}
