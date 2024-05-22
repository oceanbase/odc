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
package com.oceanbase.odc.metadb.task;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;

public class TaskRepositoryTest extends ServiceTestEnv {

    @Autowired
    private TaskRepository taskRepository;

    @Before
    public void setUp() {
        this.taskRepository.deleteAll();
    }

    @Test
    public void updateLastHeartbeatTimeById_updateSucceed_returnNotNull() {
        TaskEntity taskEntity = TestRandom.nextObject(TaskEntity.class);
        taskEntity.setId(null);
        taskEntity.setLastHeartbeatTime(null);
        taskEntity = this.taskRepository.save(taskEntity);
        this.taskRepository.updateLastHeartbeatTimeById(taskEntity.getId());
        Optional<TaskEntity> optional = this.taskRepository.findById(taskEntity.getId());
        Assert.assertNotNull(optional.get().getLastHeartbeatTime());
    }

    @Test
    public void findAllByLastHeartbeatTimeBefore_findBeforeNow_returnNotNull() {
        TaskEntity taskEntity = TestRandom.nextObject(TaskEntity.class);
        taskEntity.setId(null);
        taskEntity.setLastHeartbeatTime(null);
        taskEntity = this.taskRepository.save(taskEntity);
        this.taskRepository.updateLastHeartbeatTimeById(taskEntity.getId());
        Optional<TaskEntity> optional = this.taskRepository.findById(taskEntity.getId());
        Date heartbeatTime = new Date(optional.get().getLastHeartbeatTime().getTime() + 1);
        List<TaskEntity> actual = this.taskRepository.findAllByLastHeartbeatTimeBefore(heartbeatTime);
        Assert.assertFalse(actual.isEmpty());
    }

    @Test
    public void findAllByLastHeartbeatTimeBefore_findBeforeLastHeartbeatTime_returnEmpty() {
        TaskEntity taskEntity = TestRandom.nextObject(TaskEntity.class);
        taskEntity.setId(null);
        taskEntity.setLastHeartbeatTime(null);
        taskEntity = this.taskRepository.save(taskEntity);
        this.taskRepository.updateLastHeartbeatTimeById(taskEntity.getId());
        Optional<TaskEntity> optional = this.taskRepository.findById(taskEntity.getId());
        List<TaskEntity> actual = this.taskRepository
                .findAllByLastHeartbeatTimeBefore(optional.get().getLastHeartbeatTime());
        Assert.assertTrue(actual.isEmpty());
    }

}
