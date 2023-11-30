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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-30
 * @since 4.2.4
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class TaskFrameworkService {

    @Autowired
    private TaskRepository taskRepository;

    @Transactional(rollbackFor = Exception.class)
    public void update(TaskResult taskResult) {
        JobIdentity identity = taskResult.getJobIdentity();
        TaskEntity taskEntity = nullSafeFindById(identity.getId());
        taskEntity.setProgressPercentage(taskResult.getProgress());
        taskEntity.setStatus(taskResult.getTaskStatus() == null ? TaskStatus.RUNNING : taskResult.getTaskStatus());
        taskRepository.update(taskEntity);
    }

    private TaskEntity nullSafeFindById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", id));
    }
}
