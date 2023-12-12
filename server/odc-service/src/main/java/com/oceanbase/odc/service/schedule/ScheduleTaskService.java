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

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskResp;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

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

    @Value("${odc.log.directory:./log}")
    private String logDirectory;

    private static final String LOG_PATH_PATTERN = "%s/scheduleTask/%s-%s/%s/log.%s";

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

    public List<ScheduleTaskEntity> listTaskByJobNameAndStatus(String jobName, List<TaskStatus> statuses) {
        return scheduleTaskRepository.findByJobNameAndStatusIn(jobName, statuses);
    }


    public ScheduleTaskEntity nullSafeGetById(Long id) {
        Optional<ScheduleTaskEntity> scheduleEntityOptional = scheduleTaskRepository.findById(id);
        return scheduleEntityOptional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE_TASK, "id", id));
    }

    public String getScheduleTaskLog(Long id, OdcTaskLogLevel logLevel) {
        ScheduleTaskEntity taskEntity = nullSafeGetById(id);
        String filePath = String.format(LOG_PATH_PATTERN, logDirectory,
                taskEntity.getJobName(), taskEntity.getJobGroup(), taskEntity.getId(),
                logLevel.name().toLowerCase());

        if (!new File(filePath).exists()) {
            return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", id});
        }
        LineIterator it = null;
        StringBuilder sb = new StringBuilder();
        int lineCount = 1;
        int byteCount = 0;
        try {
            it = FileUtils.lineIterator(new File(filePath));
            while (it.hasNext()) {
                if (lineCount > 10000 || byteCount > 1024 * 1024) {
                    sb.append("Logs exceed max limitation (10000 rows or 1 MB), please download logs directly");
                    break;
                }
                String line = it.nextLine();
                sb.append(line).append("\n");
                lineCount++;
                byteCount = byteCount + line.getBytes().length;
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("read task log file failed, reason={}", ex.getMessage());
            throw new UnexpectedException("read task log file failed, reason: " + ex.getMessage(), ex);
        } finally {
            LineIterator.closeQuietly(it);
        }
    }
}
