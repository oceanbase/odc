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
package com.oceanbase.odc.core.sql.async;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.task.TaskManager;

/**
 * Test case for {@link DefaultSqlExecuteTaskManager}
 *
 * @author yh263208
 * @date 2021-11-11 17:14
 * @since ODC_release_3.2.2
 */
public class DefaultSqlExecuteTaskManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void submit_submitRunnableToOffer_pollSucceed() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>();
            Long timestamp = System.currentTimeMillis();
            manager.submit(() -> queue.offer(timestamp));
            Assert.assertEquals(timestamp, queue.poll(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void submit_submitRunnableToOfferWithTimeout_expThrown() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>();
            Long timestamp = System.currentTimeMillis();
            thrown.expect(IllegalStateException.class);
            manager.submit(() -> queue.offer(timestamp), 1, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void submit_submitCallableToReturnTime_getSucceed() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            Long timestamp = System.currentTimeMillis();
            Future<Long> future = manager.submit(() -> timestamp);
            Assert.assertEquals(timestamp, future.get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void submit_submitCallableToReturnTimeWithTimeout_expThrown() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            thrown.expect(IllegalStateException.class);
            manager.submit(System::currentTimeMillis, 1, TimeUnit.MINUTES);
        }
    }

    @Test
    public void cancel_cancelRunningTask_interruptedExpThrown() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            CountDownLatch latch = new CountDownLatch(1);
            Future<Long> future = manager.submit(() -> {
                latch.countDown();
                Thread.sleep(10000);
                return System.currentTimeMillis();
            });
            Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
            Assert.assertTrue(future.cancel(true));
            Assert.assertTrue(future.isCancelled());
            thrown.expect(CancellationException.class);
            future.get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void cancel_tryToCancelDoneTask_cancelFailed() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            Future<Long> future = manager.submit(System::currentTimeMillis);
            future.get(1, TimeUnit.SECONDS);
            Assert.assertTrue(future.isDone());
            Assert.assertFalse(future.cancel(true));
        }
    }

    @Test
    public void cancel_tryToCancelCancelledTask_cancelFailed() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            CountDownLatch latch = new CountDownLatch(1);
            Future<Long> future = manager.submit(() -> {
                latch.countDown();
                Thread.sleep(10000);
                return System.currentTimeMillis();
            });
            Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
            Assert.assertTrue(future.cancel(true));
            Assert.assertFalse(future.cancel(true));
        }
    }

    @Test
    public void submit_submitTooManyRunnable_expThrown() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            thrown.expectMessage("Too many tasks submitted, max=3");
            thrown.expect(BadRequestException.class);
            for (int i = 0; i < 4; i++) {
                manager.submit(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // eat exp
                    }
                });
            }
        }
    }

    @Test
    public void submit_submitTooManyCallable_expThrown() throws Exception {
        try (TaskManager manager = getTaskManager()) {
            thrown.expectMessage("Too many tasks submitted, max=3");
            thrown.expect(BadRequestException.class);
            for (int i = 0; i < 4; i++) {
                manager.submit(() -> {
                    Thread.sleep(1000);
                    return null;
                });
            }
        }
    }

    @Test
    public void submit_taskWithExpThrown_catchException() throws Throwable {
        try (TaskManager manager = getTaskManager()) {
            long timestamp = System.currentTimeMillis();
            Future<Long> future = manager.submit(() -> {
                throw new IllegalStateException(timestamp + "");
            });
            thrown.expect(ExecutionException.class);
            thrown.expectMessage(timestamp + "");
            future.get(1, TimeUnit.SECONDS);
        }
    }

    private TaskManager getTaskManager() {
        return new DefaultSqlExecuteTaskManager(3, "test", 1, TimeUnit.MILLISECONDS);
    }

}
