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
package com.oceanbase.odc.core.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManagerFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Test case for {@link ExecuteMonitorTaskManager}
 *
 * @author yh263208
 * @date 2021-11-11 10:31
 * @since ODC_release_3.2.2
 */
@Slf4j
public class ExecuteMonitorTaskManagerTest {

    private final static ExecuteMonitorTaskManager monitorTaskManager = new ExecuteMonitorTaskManager();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @AfterClass
    public static void clear() {
        monitorTaskManager.close();
    }

    @Test
    public void submit_runnableNotExceedTimeout_executedSucceed() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>();
            Long timestamp = System.currentTimeMillis();
            manager.submit(() -> queue.offer(timestamp), 1, TimeUnit.SECONDS);
            Assert.assertEquals(timestamp, queue.poll(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void submit_runnableExceedTimeout_taskIsInterrupted() throws Exception {
        try (TaskManager manager = getTaskManager("SubmitRunnableTaskWithTimeout")) {
            LinkedBlockingQueue<InterruptedException> queue = new LinkedBlockingQueue<>();
            manager.submit(() -> {
                try {
                    busyWaiting(1500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    queue.offer(e);
                }
            }, 1, TimeUnit.SECONDS);
            Assert.assertNotNull(queue.poll(5000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void submit_callableNotExceedTimeout_executedSucceed() throws Exception {
        try (TaskManager manager = getTaskManager("SubmitCallableTaskWithoutTimeout")) {
            Long timestamp = System.currentTimeMillis();
            Future<Long> future = manager.submit(() -> timestamp, 1, TimeUnit.SECONDS);
            Assert.assertEquals(timestamp, future.get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void submit_callableExceedTimeout_taskIsInterrupted() throws Exception {
        try (TaskManager manager = getTaskManager("SubmitCallableTaskWithTimeout")) {
            Future<String> future = manager.submit(() -> {
                busyWaiting(1500, TimeUnit.MILLISECONDS);
                return "Hello,world";
            }, 1, TimeUnit.SECONDS);
            thrown.expect(CancellationException.class);
            future.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void cancel_cancelRunningTask_calcelSucceed() throws Exception {
        try (TaskManager manager = getTaskManager("testStopTask")) {
            Future<Long> future = manager.submit(() -> {
                busyWaiting(10, TimeUnit.SECONDS);
                return System.currentTimeMillis();
            });
            Assert.assertTrue(future.cancel(true));
            thrown.expect(CancellationException.class);
            future.get(1, TimeUnit.SECONDS);
        }
    }

    private void busyWaiting(long time, @NonNull TimeUnit timeUnit) throws InterruptedException {
        long waitingTime = TimeUnit.MILLISECONDS.convert(time, timeUnit);
        long destTimtstamp = System.currentTimeMillis() + waitingTime;
        while (!Thread.currentThread().isInterrupted()) {
            if (System.currentTimeMillis() - destTimtstamp > 0) {
                break;
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private TaskManager getTaskManager() {
        SqlExecuteTaskManagerFactory factory =
                new SqlExecuteTaskManagerFactory(monitorTaskManager, null, 3, 1, TimeUnit.MILLISECONDS);
        return factory.generateManager();
    }

    private TaskManager getTaskManager(@NonNull String taskManagerName) {
        SqlExecuteTaskManagerFactory factory =
                new SqlExecuteTaskManagerFactory(monitorTaskManager, taskManagerName, 3, 1, TimeUnit.MILLISECONDS);
        return factory.generateManager();
    }

}
