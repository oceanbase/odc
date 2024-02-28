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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponents;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
@Slf4j
@Service
public class ScheduleTaskResultHandleService implements ResultHandleService {

    @Autowired
    private ScheduleTaskRepository taskRepository;

    @Autowired
    private StdTaskFrameworkService stdTaskFrameworkService;

    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handle(TaskResult taskResult) {
        JobIdentity identity = taskResult.getJobIdentity();
        JobEntity jobEntity = stdTaskFrameworkService.find(identity.getId());

        Optional<ScheduleTaskEntity> taskEntityOptional = taskRepository.findByJobId(jobEntity.getId());
        if (taskEntityOptional.isPresent()) {
            ScheduleTaskEntity taskEntity = taskEntityOptional.get();
            taskEntity.setProgressPercentage(taskResult.getProgress());
            taskEntity.setStatus(taskResult.getStatus().convertTaskStatus());
            taskEntity.setResultJson(taskResult.getResultJson());
            if (taskResult.getExecutorEndpoint() != null) {
                UriComponents uc = UrlUtils.getUriComponents(taskResult.getExecutorEndpoint());
                ExecutorInfo executorInfo = new ExecutorInfo();
                executorInfo.setHost(uc.getHost());
                executorInfo.setPort(uc.getPort());
                taskEntity.setExecutor(JsonUtils.toJson(executorInfo));
            }
            taskRepository.update(taskEntity);
            log.info("Update task {} successfully.", taskEntity.getId());
        }
    }


}
