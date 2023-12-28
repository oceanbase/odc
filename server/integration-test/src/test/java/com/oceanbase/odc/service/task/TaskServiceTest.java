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
package com.oceanbase.odc.service.task;

import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.QueryTaskInstanceParams;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.OdcMockTaskConfig;

/**
 * @author wenniu.ly
 * @date 2022/2/18
 */
public class TaskServiceTest extends ServiceTestEnv {
    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    private final int defaultExpireInterval = 10;

    @Before
    public void setUp() throws Exception {
        taskRepository.deleteAll();
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void test_create() {
        taskService.create(createTaskReq(), defaultExpireInterval);
        Assert.assertEquals(1, taskRepository.count());
    }

    @Test
    public void test_list() {
        long id = taskService.create(createTaskReq(), defaultExpireInterval).getId();
        taskService.start(id);
        taskService.create(createTaskReq(), defaultExpireInterval).getId();

        taskService.create(createMockTaskReq(), defaultExpireInterval);
        taskService.create(createMockTaskReq(), defaultExpireInterval);
        QueryTaskInstanceParams params =
                QueryTaskInstanceParams.builder().statuses(Arrays.asList(TaskStatus.RUNNING)).build();
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Direction.DESC, "createTime"));
        Page<TaskEntity> taskEntities = taskService.list(pageable, params);
        Assert.assertEquals(1, taskEntities.getTotalElements());
        Assert.assertEquals(TaskType.ASYNC, taskEntities.getContent().get(0).getTaskType());
    }

    @Test
    public void test_detail() {
        long id = taskService.create(createTaskReq(), defaultExpireInterval).getId();
        TaskEntity taskEntity = taskService.detail(id);
        Assert.assertEquals(2, taskEntity.getConnectionId().longValue());
        Assert.assertEquals("test_database", taskEntity.getDatabaseName());
        Assert.assertEquals("select * from dual;",
                JsonUtils.fromJson(taskEntity.getParametersJson(), Map.class).get("sqlContent"));
        Assert.assertEquals(10000, JsonUtils.fromJson(taskEntity.getParametersJson(), Map.class).get("queryLimit"));
        Assert.assertEquals("test_task", taskEntity.getDescription());
    }

    @Test
    public void test_start() {
        long id = taskService.create(createTaskReq(), defaultExpireInterval).getId();
        TaskEntity taskEntity = taskService.detail(id);
        Assert.assertEquals(TaskStatus.PREPARING, taskEntity.getStatus());
        taskService.start(id);
        taskEntity = taskService.detail(id);
        Assert.assertEquals(TaskStatus.RUNNING, taskEntity.getStatus());
    }

    @Test
    public void test_end() {
        long id = taskService.create(createTaskReq(), defaultExpireInterval).getId();
        taskService.succeed(id, null);
        TaskEntity taskEntity = taskService.detail(id);
        Assert.assertEquals(TaskStatus.DONE, taskEntity.getStatus());
    }

    @Test
    public void test_cancel() {
        long id = taskService.create(createTaskReq(), defaultExpireInterval).getId();
        taskService.cancel(id);
        TaskEntity taskEntity = taskService.detail(id);
        Assert.assertEquals(TaskStatus.CANCELED, taskEntity.getStatus());
    }

    @Test
    public void test_delete() {
        long id = taskService.create(createTaskReq(), defaultExpireInterval).getId();
        Assert.assertEquals(1, taskRepository.count());
        taskService.delete(id);
        Assert.assertEquals(0, taskRepository.count());
    }

    @Test
    public void test_update() {
        TaskEntity taskEntity = taskService.create(createTaskReq(), defaultExpireInterval);
        Long riskLevel = 1L;
        taskEntity.setRiskLevelId(riskLevel);
        taskService.update(taskEntity);
        TaskEntity detail = taskService.detail(taskEntity.getId());
        Assert.assertEquals(riskLevel, detail.getRiskLevelId());
    }

    private CreateFlowInstanceReq createTaskReq() {
        CreateFlowInstanceReq req = new CreateFlowInstanceReq();
        req.setConnectionId(2L);
        req.setDatabaseName("test_database");
        req.setTaskType(TaskType.ASYNC);
        DatabaseChangeParameters parameters1 = new DatabaseChangeParameters();
        parameters1.setSqlContent("select * from dual;");
        parameters1.setQueryLimit(10000);
        req.setParameters(parameters1);
        req.setDescription("test_task");
        return req;
    }

    private CreateFlowInstanceReq createMockTaskReq() {
        CreateFlowInstanceReq req = new CreateFlowInstanceReq();
        req.setConnectionId(2L);
        req.setDatabaseName("test_database");
        req.setParameters(new OdcMockTaskConfig());
        req.setTaskType(TaskType.ASYNC);
        req.setDescription("test_task");
        return req;
    }

}
