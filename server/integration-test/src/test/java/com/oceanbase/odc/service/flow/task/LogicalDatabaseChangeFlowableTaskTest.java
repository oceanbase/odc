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
package com.oceanbase.odc.service.flow.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.LogicalDatabaseChangeParameters;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.base.logicdatabasechange.LogicalDatabaseChangeTask;

/**
 * 单元测试类：LogicalDatabaseChangeFlowableTask
 *
 * @author Yizhuo
 * @date 2025/06/10 18:12:07
 */
@RunWith(MockitoJUnitRunner.class)
public class LogicalDatabaseChangeFlowableTaskTest {

    @Mock
    private TaskService taskService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private LogicalDatabaseService logicalDatabaseService;

    @Mock
    private LogicalDatabaseChangeTask logicalDatabaseChangeTask;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private LogicalDatabaseChangeFlowableTask flowableTask;

    private TaskEntity mockTaskEntity;
    private LogicalDatabaseChangeParameters mockParameters;
    private DetailLogicalDatabaseResp mockLogicalDatabaseResp;
    private ConnectionConfig mockConnectionConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化测试数据
        mockTaskEntity = new TaskEntity();
        mockTaskEntity.setId(1L);
        mockTaskEntity.setCreatorId(100L);
        mockTaskEntity.setParametersJson(
                "{\"sqlContent\":\"CREATE TABLE test (id INT);\",\"delimiter\":\";\",\"timeoutMillis\":30000,\"databaseId\":1}");

        mockParameters = new LogicalDatabaseChangeParameters();
        mockParameters.setSqlContent("CREATE TABLE test (id INT);");
        mockParameters.setDelimiter(";");
        mockParameters.setTimeoutMillis(30000L);
        mockParameters.setDatabaseId(1L);

        mockLogicalDatabaseResp = new DetailLogicalDatabaseResp();
        mockLogicalDatabaseResp.setId(1L);
        mockLogicalDatabaseResp.setName("test_logical_db");
        mockLogicalDatabaseResp.setDialectType(DialectType.OB_MYSQL);

        List<Database> physicalDatabases = new ArrayList<>();
        Database physicalDb = new Database();
        physicalDb.setId(1L);
        physicalDb.setName("test_physical_db");
        physicalDb.setDataSource(mockConnectionConfig);
        physicalDatabases.add(physicalDb);
        mockLogicalDatabaseResp.setPhysicalDatabases(physicalDatabases);

        mockConnectionConfig = new ConnectionConfig();
        mockConnectionConfig.setId(1L);
        mockConnectionConfig.setName("test_connection");
    }

    @Test
    public void testCancel_Success() {
        // 准备测试数据
        LogicalDatabaseChangeTask mockTask = mock(LogicalDatabaseChangeTask.class);
        setField(flowableTask, "logicalDatabaseChangeTask", mockTask);

        // 执行测试
        boolean result = flowableTask.cancel(true, 1L, taskService);

        // 验证结果
        assertTrue(result);
        verify(mockTask).stop();
    }

    @Test
    public void testCancel_NullTask() {
        // 执行测试
        boolean result = flowableTask.cancel(true, 1L, taskService);

        // 验证结果
        assertTrue(result);
        // 验证stop方法没有被调用，因为task为null
    }

    @Test
    public void testIsCancelled() {
        // 测试初始状态
        assertFalse(flowableTask.isCancelled());

        // 设置取消状态
        setField(flowableTask, "isCancelled", new AtomicBoolean(true));

        // 验证结果
        assertTrue(flowableTask.isCancelled());
    }

    @Test
    public void testIsSuccessful() {
        // 测试初始状态
        assertFalse(flowableTask.isSuccessful());

        // 设置成功状态
        setField(flowableTask, "isSuccessful", new AtomicBoolean(true));

        // 验证结果
        assertTrue(flowableTask.isSuccessful());
    }

    @Test
    public void testIsFailure_NotCancelledNotSuccessfulWithException() {
        // 设置状态：未取消、未成功、有异常
        setField(flowableTask, "isCancelled", new AtomicBoolean(false));
        setField(flowableTask, "isSuccessful", new AtomicBoolean(false));
        setField(flowableTask, "exception", new AtomicReference<>(new RuntimeException("测试异常")));

        // 验证结果
        assertTrue(flowableTask.isFailure());
    }

    @Test
    public void testIsFailure_Cancelled() {
        // 设置状态：已取消
        setField(flowableTask, "isCancelled", new AtomicBoolean(true));
        setField(flowableTask, "isSuccessful", new AtomicBoolean(false));
        setField(flowableTask, "exception", new AtomicReference<>(new RuntimeException("测试异常")));

        // 验证结果
        assertFalse(flowableTask.isFailure());
    }

    @Test
    public void testIsFailure_Successful() {
        // 设置状态：已成功
        setField(flowableTask, "isCancelled", new AtomicBoolean(false));
        setField(flowableTask, "isSuccessful", new AtomicBoolean(true));
        setField(flowableTask, "exception", new AtomicReference<>(new RuntimeException("测试异常")));

        // 验证结果
        assertFalse(flowableTask.isFailure());
    }

    @Test
    public void testIsFailure_NoException() {
        // 设置状态：未取消、未成功、无异常
        setField(flowableTask, "isCancelled", new AtomicBoolean(false));
        setField(flowableTask, "isSuccessful", new AtomicBoolean(false));
        setField(flowableTask, "exception", new AtomicReference<>(null));

        // 验证结果
        assertFalse(flowableTask.isFailure());
    }

    @Test
    public void testStopTask_WithTask() {
        // 准备测试数据
        LogicalDatabaseChangeTask mockTask = mock(LogicalDatabaseChangeTask.class);
        setField(flowableTask, "logicalDatabaseChangeTask", mockTask);

        // 执行测试
        flowableTask.testStopTask();

        // 验证结果
        verify(mockTask).stop();
    }

    @Test
    public void testStopTask_WithoutTask() {
        // 执行测试
        flowableTask.testStopTask();

        // 验证结果：当task为null时，不调用stop方法
        // 这里主要是验证方法不会抛出异常
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
