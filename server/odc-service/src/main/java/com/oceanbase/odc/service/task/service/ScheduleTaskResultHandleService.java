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
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.task.executor.task.TaskResult;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
@Service
public class ScheduleTaskResultHandleService implements ResultHandleService {

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;


    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handle(TaskResult taskResult) {

    }


}
