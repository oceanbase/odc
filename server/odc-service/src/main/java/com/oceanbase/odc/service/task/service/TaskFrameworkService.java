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

import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobDefinition;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
public interface TaskFrameworkService {
    @Transactional(rollbackFor = Exception.class)
    void handleResult(TaskResult taskResult);

    @Transactional(rollbackFor = Exception.class)
    JobEntity save(JobDefinition jd);

    JobEntity find(Long id);

    JobDefinition getJobDefinition(Long id);

    void startSuccess(Long id, String serialNumber);

    void updateScheduleTimes(Long id, Integer scheduleTimes);

    void updateDescription(Long id, String description);

    void update(JobEntity jobEntity);

}
