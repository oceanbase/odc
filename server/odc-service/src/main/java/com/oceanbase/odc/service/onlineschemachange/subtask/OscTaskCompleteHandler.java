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
package com.oceanbase.odc.service.onlineschemachange.subtask;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OscTaskCompleteHandler {

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    private OmsResourceCleanHandler resourceCleanHandler;

    public void onOscScheduleTaskFailed(String omsProjectId, String uid, Long scheduleId,
            Long scheduleTaskId) {
        proceed(omsProjectId, uid, scheduleId, scheduleTaskId, TaskStatus.FAILED);
    }

    public void onOscScheduleTaskSuccess(String omsProjectId, String uid, Long scheduleId, Long scheduleTaskId) {
        proceed(omsProjectId, uid, scheduleId, scheduleTaskId, TaskStatus.DONE);
    }

    public void onOscScheduleTaskCancel(String omsProjectId, String uid, Long scheduleId, Long scheduleTaskId) {
        proceed(omsProjectId, uid, scheduleId, scheduleTaskId, TaskStatus.CANCELED);
    }

    private void proceed(String omsProjectId, String uid, Long scheduleId, Long scheduleTaskId,
            TaskStatus status) {
        try {
            updateScheduleTask(scheduleTaskId, status);
            ProjectControlRequest projectControlRequest = new ProjectControlRequest();
            projectControlRequest.setUid(uid);
            projectControlRequest.setId(omsProjectId);
            resourceCleanHandler.checkAndReleaseProject(projectControlRequest);
        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "Failed to proceed, schedule id {0}", scheduleId),
                    e);
        }
    }


    private void updateScheduleTask(Long scheduleTaskId, TaskStatus status) {
        if (TaskStatus.DONE == status) {
            scheduleTaskRepository.updateStatusAndProcessPercentageById(scheduleTaskId, status, 100D);
        } else {
            scheduleTaskRepository.updateStatusById(scheduleTaskId, status);
        }
        log.info("Successfully update schedule task id {} set status {}", scheduleTaskId, status);
    }

}
