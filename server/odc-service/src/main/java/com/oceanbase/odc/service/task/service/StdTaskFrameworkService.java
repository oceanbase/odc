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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.task.enums.SourceType;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
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

    private Map<SourceType, ResultHandleService> handlers;

    @PostConstruct
    private void initResultHandler() {
        handlers = new HashMap<>();
        handlers.put(SourceType.TASK_TASK, SpringContextUtil.getBean(TaskTaskResultHandleService.class));
        handlers.put(SourceType.SCHEDULE_TASK, SpringContextUtil.getBean(ScheduleTaskResultHandleService.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(TaskResult taskResult) {
        if (taskResult.getTaskStatus().isTerminated()) {
            log.warn("task is terminated, ignore upload result.{}", JsonUtils.toJson(taskResult));
            return;
        }
        JobIdentity identity = taskResult.getJobIdentity();
        getResultHandle(identity.getSourceType()).handle(taskResult);
        updateJobScheduleEntity(taskResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(JobDefinition jd) {
        JobScheduleEntity jse = new JobScheduleEntity();
        JobIdentity ji = jd.getJobIdentity();
        jse.setSourceId(ji.getSourceId());
        jse.setSourceType(ji.getSourceType());
        jse.setSourceSubType(ji.getSourceSubType());
        jse.setMisfireStrategy(jd.getMisfireStrategy());
        jse.setTriggerConfigJson(JsonUtils.toJson(jd.getTriggerConfig()));
        jse.setJobDataJson(JsonUtils.toJson(jd.getJobData()));
        jse.setScheduleTimes(0);
        jobScheduleRepository.save(jse);
    }


    @Override
    public JobDefinition find(JobIdentity ji) {
        JobScheduleEntity jse = jobScheduleRepository.findBySourceIdAndSourceType(ji.getSourceId(), ji.getSourceType());
        return DefaultJobDefinition.builder()
                .jobIdentity(JobIdentity.of(jse.getSourceId(), jse.getSourceType(), jse.getSourceSubType()))
                .jobData(JsonUtils.fromJson(jse.getJobDataJson(), new TypeReference<Map<String, String>>() {}))
                .triggerConfig(JsonUtils.fromJson(jse.getTriggerConfigJson(), new TypeReference<TriggerConfig>() {}))
                .misfireStrategy(jse.getMisfireStrategy())
                .build();

    }


    private JobScheduleEntity findJobEntity(JobIdentity ji) {
        return jobScheduleRepository.findBySourceIdAndSourceType(ji.getSourceId(), ji.getSourceType());
    }


    private void updateJobScheduleEntity(TaskResult taskResult) {
        JobScheduleEntity jse = findJobEntity(taskResult.getJobIdentity());
        updateStatusAndScheduleTimes(jse.getId(), taskResult.getTaskStatus(), jse.getScheduleTimes() + 1);
    }

    private ResultHandleService getResultHandle(SourceType sourceType) {
        ResultHandleService rhs = handlers.get(sourceType);
        if (rhs == null) {
            throw new IllegalArgumentException("Not found source type " + sourceType + " to handle result.");
        }
        return rhs;
    }

    private void updateStatusAndScheduleTimes(Long id, TaskStatus status, Integer scheduleTimes) {
        jobScheduleRepository.updateStatusAndScheduleTimesById(id, status, scheduleTimes);
    }

}
