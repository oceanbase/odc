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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.input.ReversedLinesFileReader;
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
import com.oceanbase.odc.metadb.schedule.ScheduleTaskUnitRepository;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.model.DataArchiveExecutionDetail;
import com.oceanbase.odc.service.schedule.model.DataArchiveTaskUnitParameters;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskResp;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskUnit;
import com.oceanbase.odc.service.schedule.utils.ScheduleTaskUnitMapper;
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
    private ScheduleTaskUnitRepository scheduleTaskUnitRepository;

    @Autowired
    private QuartzJobService quartzJobService;

    private final ScheduleTaskMapper scheduleTaskMapper = ScheduleTaskMapper.INSTANCE;

    @Value("${odc.log.directory:./log}")
    private String logDirectory;

    private static final String LOG_PATH_PATTERN = "%s/scheduleTask/%s-%s/%s/log.%s";

    public ScheduleTaskResp detail(Long id) {
        ScheduleTaskEntity entity = nullSafeGetById(id);
        ScheduleTaskResp scheduleTaskResp = scheduleTaskMapper.entityToModel(entity);
        List<ScheduleTaskUnit> taskUnits = scheduleTaskUnitRepository.findByScheduleTaskId(id).stream().map(
                ScheduleTaskUnitMapper::toModel).collect(
                        Collectors.toList());

        ScheduleTaskUnit taskUnit = new ScheduleTaskUnit();
        DataArchiveTaskUnitParameters dataArchiveTaskUnitParameters = new DataArchiveTaskUnitParameters();
        dataArchiveTaskUnitParameters.setId(1L);
        taskUnit.setTaskUnitParameters(dataArchiveTaskUnitParameters);
        DataArchiveExecutionDetail detail = new DataArchiveExecutionDetail();
        detail.setAvgReadRowCount(100L);
        detail.setAvgWriteRowCount(100L);
        detail.setStatus("RUNNING");
        detail.setTableName("table_name");
        detail.setProcessRowCount(10000L);
        detail.setUserCondition("create_time < '2022-09-01'");
        detail.setReadRowCount(20000L);
        detail.setType(JobType.DATA_ARCHIVE.name());
        taskUnit.setExecutionDetail(detail);
        taskUnit.setStartTime(new Date());
        taskUnit.setEndTime(new Date());
        taskUnit.setType(JobType.DATA_ARCHIVE);
        taskUnit.setScheduleTaskId(id);
        taskUnit.setId(1L);
        taskUnits.add(taskUnit);
        taskUnits.add(taskUnit);
        scheduleTaskResp.setTaskUnits(taskUnits);
        return scheduleTaskResp;
    }

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


    public Optional<ScheduleTaskEntity> findByJobId(Long jobId) {
        List<ScheduleTaskEntity> scheduleTasks = scheduleTaskRepository.findByJobId(jobId);
        if (scheduleTasks != null) {
            if (scheduleTasks.size() > 1) {
                throw new IllegalStateException("Query scheduleTask by jobId occur error, except 1 but found "
                        + scheduleTasks.size() + ",jobId=" + jobId);
            } else if (scheduleTasks.size() == 1) {
                return Optional.of(scheduleTasks.get(0));
            }
        }
        return Optional.empty();
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
        File logFile = new File(filePath);
        if (!logFile.exists()) {
            return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {"Id", id});
        }
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<>();
            int bytes = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                bytes += line.getBytes().length;
                if (lines.size() >= 10000 || bytes >= 1024 * 1024) {
                    lines.add("[ODC INFO]: \n"
                            + "Logs exceed max limitation (10000 rows or 1 MB), only the latest part is displayed.\n"
                            + "Please download the log file for the full content.");
                    break;
                }
            }
            StringBuilder logBuilder = new StringBuilder();
            for (int i = lines.size() - 1; i >= 0; i--) {
                logBuilder.append(lines.get(i)).append("\n");
            }
            return logBuilder.toString();
        } catch (Exception ex) {
            log.warn("Read task log file failed, details={}", ex.getMessage());
            throw new UnexpectedException("Read task log file failed, details: " + ex.getMessage(), ex);
        }
    }
}
