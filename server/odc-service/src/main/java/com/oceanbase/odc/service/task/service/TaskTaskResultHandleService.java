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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
@Service
public class TaskTaskResultHandleService implements ResultHandleService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private StdTaskFrameworkService stdTaskFrameworkService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handle(TaskResult taskResult) {
        JobIdentity identity = taskResult.getJobIdentity();
        JobEntity jobEntity = stdTaskFrameworkService.find(identity.getId());
        Map<String, String> jobData =
                JsonUtils.fromJson(jobEntity.getJobDataJson(), new TypeReference<Map<String, String>>() {});
        String taskTaskIdString = jobData.get(JobDataMapConstants.BUZ_ID);
        if (taskTaskIdString != null) {
            Long taskTaskId = Long.parseLong(taskTaskIdString);
            TaskEntity taskEntity = taskService.detail(taskTaskId);
            taskEntity.setProgressPercentage(taskResult.getProgress() * 100);
            taskEntity.setStatus(taskResult.getTaskStatus());
            taskEntity.setResultJson(taskResult.getResultJson());
            taskService.update(taskEntity);
        }

    }
}
