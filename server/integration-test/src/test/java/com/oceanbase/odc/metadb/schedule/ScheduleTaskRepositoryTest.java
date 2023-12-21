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
package com.oceanbase.odc.metadb.schedule;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.TaskStatus;

import cn.hutool.core.lang.Assert;

/**
 * @Author：tinker
 * @Date: 2023/5/24 17:43
 * @Descripition:
 */
public class ScheduleTaskRepositoryTest extends ServiceTestEnv {


    @Autowired
    private ScheduleTaskRepository taskRepository;

    @Test
    public void create() {
        ScheduleTaskEntity scheduleTask = createScheduleTask();
        Assert.isTrue(scheduleTask.getId() != null);
    }

    @Test
    public void updateStatusById() {
        ScheduleTaskEntity scheduleTask = createScheduleTask();
        taskRepository.updateStatusById(scheduleTask.getId(), TaskStatus.DONE);
        Optional<ScheduleTaskEntity> optional = taskRepository.findById(scheduleTask.getId());
        Assert.equals(optional.get().getStatus(), TaskStatus.DONE);
    }

    @Test
    public void updateExecutor() {
        ScheduleTaskEntity scheduleTask = createScheduleTask();
        String executor = "expect";
        taskRepository.updateExecutor(scheduleTask.getId(), executor);
        Optional<ScheduleTaskEntity> byId = taskRepository.findById(scheduleTask.getId());
        Assert.equals(executor, byId.get().getExecutor());
    }

    @Test
    public void listByScheduleIdAndStatus() {
        taskRepository.deleteAll();
        createScheduleTask();
        List<TaskStatus> statuses = new LinkedList<>();
        statuses.add(TaskStatus.RUNNING);
        statuses.add(TaskStatus.PREPARING);
        List<ScheduleTaskEntity> byJobNameAndStatus = taskRepository.findByJobNameAndStatusIn("1", statuses);
        Assert.equals(byJobNameAndStatus.size(), 1);
    }

    private ScheduleTaskEntity createScheduleTask() {
        ScheduleTaskEntity entity = new ScheduleTaskEntity();
        entity.setStatus(TaskStatus.PREPARING);
        entity.setParametersJson("");
        entity.setJobName("1");
        entity.setJobGroup("MIGRATION");
        entity.setFireTime(new Date());
        return taskRepository.save(entity);
    }
}
