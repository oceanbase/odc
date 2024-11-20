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
package com.oceanbase.odc.service.task.processor.result;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.dlm.DLMService;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.processor.matcher.DLMProcessorMatcher;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/7/6 10:45
 * @Descripition:
 */

@Slf4j
@Component
public class DLMResultProcessor extends DLMProcessorMatcher implements ResultProcessor {

    @Autowired
    protected DLMService dlmService;

    @Autowired
    protected ScheduleTaskService taskService;

    @Override
    public void process(TaskResult result) {
        log.info("Start refresh result,result={}", result.getResultJson());
        try {
            List<DlmTableUnit> dlmTableUnits = JsonUtils.fromJson(result.getResultJson(),
                    new TypeReference<List<DlmTableUnit>>() {});
            if (dlmTableUnits == null || dlmTableUnits.isEmpty()) {
                log.warn("Task result is empty!jobIdentity={}", result.getJobIdentity());
                return;
            }
            dlmService.createOrUpdateDlmTableUnits(dlmTableUnits);
            log.info("Create or update dlm tableUnits success,jobIdentity={},scheduleTaskId={}",
                    result.getJobIdentity(),
                    dlmTableUnits.get(0).getScheduleTaskId());
            TaskStatus taskStatus = taskService.nullSafeGetById(dlmTableUnits.get(0).getScheduleTaskId()).getStatus();
            if (taskStatus != TaskStatus.RUNNING) {
                taskService.updateStatusById(dlmTableUnits.get(0).getScheduleTaskId(), TaskStatus.RUNNING);
            }
            log.info("Update schedule task status to {} success", taskStatus);
        } catch (Exception e) {
            log.warn("Refresh result failed.", e);
        }

    }
}
