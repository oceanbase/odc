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
package com.oceanbase.odc.service.notification;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

import com.oceanbase.odc.core.task.TaskThreadFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: ysj
 * @Date: 2025/3/6 14:18
 * @Since: 4.3.4
 * @Description: The event of notification is submitted asynchronously through the broker, ensuring
 *               no duplication and timeout limits
 */
@Slf4j
public class NotificationEventTaskExecutor {

    private final static int CORE_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors() << 1, 8);
    // Set a unique value to prevent repeated submission of threaded tasks
    private final static Set<String> submittedIds = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor executor;
    private static final NotificationEventTaskExecutor TASK_EXECUTOR = new NotificationEventTaskExecutor();
    private final ScheduledExecutorService timeoutScheduler;
    private final static long DEFAULT_TIMEOUT = 8000;
    private final static TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    private NotificationEventTaskExecutor() {
        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                CORE_POOL_SIZE,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new TaskThreadFactory("notification-event-thread"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        timeoutScheduler = Executors.newScheduledThreadPool(2);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static NotificationEventTaskExecutor getInstance() {
        return TASK_EXECUTOR;
    }

    public void submit(@NonNull String id, @NonNull Runnable task) {
        this.submit(id, task, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
    }

    public void submit(@NonNull String id, @NonNull Runnable task, long timeout, TimeUnit timeUnit) {
        if (submittedIds.add(id)) {
            boolean isTaskSubmitted = false;
            try {
                log.info("Start to submit Task of Notification Event, id={}", id);
                Runnable wrappedTask = new DelegatingSecurityContextRunnable(task);
                CompletableFuture<Void> future = CompletableFuture.runAsync(wrappedTask, executor);

                isTaskSubmitted = true;

                timeoutScheduler.schedule(() -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(
                                new TimeoutException(String.format("Task timed out after %d %s", timeout, timeUnit)));
                    }
                }, timeout, timeUnit);
                future.whenComplete((result, ex) -> {
                    log.info("Task of Notification Event Completed, id={}", id);
                    submittedIds.remove(id);
                    if (ex != null) {
                        log.warn("Task of Notification Event execution failed, id = {}", id, ex);
                    }
                });
            } catch (Exception e) {
                log.warn("Scheduling task failed, id={}", id, e);
            } finally {
                if (!isTaskSubmitted) {
                    submittedIds.remove(id);
                    log.warn("Task of Notification Event submission failed and id has been removed, id={}", id);
                }
            }
        } else {
            log.warn("Task already submitted or is being processed, id={}", id);
        }
    }

    public void shutdown() {
        log.info("Shutting down the Task of Notification Event thread pool...");
        executor.shutdown();
        timeoutScheduler.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!timeoutScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                timeoutScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("shutdown executor interrupted", e);
            executor.shutdownNow();
            timeoutScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Task of Notification Event Thread pool has been shutdown.");
    }
}
