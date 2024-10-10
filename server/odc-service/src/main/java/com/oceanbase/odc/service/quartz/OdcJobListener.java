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
package com.oceanbase.odc.service.quartz;

import java.util.Objects;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.flowtask.ScheduleTaskContextHolder;
import com.oceanbase.odc.service.schedule.model.CreateScheduleTaskParams;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/1 11:23
 * @Descripition: Execute before or after schedule job.
 */

@Slf4j
public class OdcJobListener implements JobListener {

    private final ScheduleService scheduleService;
    private final UserService userService;
    private final ScheduleTaskService scheduleTaskService;

    private static final String ODC_JOB_LISTENER = "ODC_JOB_LISTENER";

    public OdcJobListener() {
        this.userService = SpringContextUtil.getBean(UserService.class);
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.scheduleTaskService = SpringContextUtil.getBean(ScheduleTaskService.class);
    }

    @Override
    public String getName() {
        return ODC_JOB_LISTENER;
    }

    /**
     * This method will be executed before job execute.
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        if (Objects.equals(context.getJobDetail().getKey().getGroup(),
                ScheduleType.ONLINE_SCHEMA_CHANGE_COMPLETE.name())) {
            return;
        }
        // Init user.
        Long scheduleId = ScheduleTaskUtils.getScheduleId(context);
        Schedule schedule = scheduleService.nullSafeGetModelById(scheduleId);
        log.info("Job to be executed.OrganizationId={},ProjectId={},DatabaseId={},JobType={}",
                schedule.getOrganizationId(), schedule.getProjectId(), schedule.getDatabaseId(),
                schedule.getType());
        UserEntity userEntity = userService.nullSafeGet(schedule.getCreatorId());
        userEntity.setOrganizationId(schedule.getOrganizationId());
        User taskCreator = new User(userEntity);
        SecurityContextUtils.setCurrentUser(taskCreator);

        // Create or load task.
        Long targetTaskId = ScheduleTaskUtils.getTargetTaskId(context);
        CreateScheduleTaskParams params = new CreateScheduleTaskParams();
        params.setScheduleId(scheduleId);
        params.setTaskType(ScheduleTaskType.valueOf(context.getJobDetail().getKey().getGroup()));
        String taskParameters =
                JsonUtils.toJson(useScheduleTaskParameters(params.getTaskType()) ? schedule.getParameters()
                        : context.getJobDetail().getJobDataMap());
        params.setTaskParameters(taskParameters);
        params.setFireTime(context.getFireTime());
        params.setScheduleTaskId(targetTaskId);
        ScheduleTask scheduleNextTask = scheduleService.createScheduleNextTask(params);
        ScheduleTaskContextHolder.trace(schedule.getId(), scheduleNextTask.getJobGroup(), scheduleNextTask.getId());
        ScheduleTaskUtils.setScheduleTaskId(scheduleNextTask.getId(), context);
        ScheduleTaskUtils.setScheduleTaskParameters(scheduleNextTask.getParameters(), context);
    }

    private boolean useScheduleTaskParameters(ScheduleTaskType taskType) {
        return taskType != ScheduleTaskType.DATA_ARCHIVE_DELETE && taskType != ScheduleTaskType.DATA_ARCHIVE_ROLLBACK;
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        ScheduleTaskContextHolder.clear();
        try {
            ScheduleTask scheduleTask = scheduleTaskService.nullSafeGetModelById(
                    ScheduleTaskUtils.getScheduleTaskId(context));;
            ScheduleTaskType taskType = ScheduleTaskType.valueOf(scheduleTask.getJobGroup());
            if (!taskType.isExecuteInTaskFramework()) {
                scheduleTaskService.updateStatusById(scheduleTask.getId(), TaskStatus.DONE);
            }
        } catch (Exception e) {
            log.warn("Fail to update schedule task status to done,jobKey={}", context.getJobDetail().getKey(), e);
        }
    }

}
