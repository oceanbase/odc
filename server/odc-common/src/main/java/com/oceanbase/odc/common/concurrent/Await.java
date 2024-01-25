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

package com.oceanbase.odc.common.concurrent;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-09
 * @since 4.2.0
 */
@Slf4j
@Data
@Builder(builderMethodName = "await")
public class Await {
    @Builder.Default
    private Supplier<Boolean> until = () -> true;
    @Builder.Default
    private Integer timeout = 30;
    @Builder.Default
    private Integer period = 5;
    @Builder.Default
    private String threadName = "await-thread-%d";
    // await timeout time unit
    @Builder.Default
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    // period check condition time unit
    @Builder.Default
    private TimeUnit periodTimeUnit = TimeUnit.SECONDS;
    @Builder.Default
    private Integer maxRetryTimes = Integer.MAX_VALUE;

    /**
     * The start method will be blocked until condition been matched, throw CompletionException if
     * timeout or exceed max retry times.
     */
    public void start() {
        scheduleCheck();
    }

    private void scheduleCheck() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(threadName)
                .build();
        AtomicBoolean conditionResult = new AtomicBoolean(false);
        AtomicInteger hasRetryTimes = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ScheduledThreadPoolExecutor scheduleExecutor =
                new ScheduledThreadPoolExecutor(1, threadFactory);
        try {
            ScheduledFuture<?> scheduledFuture = getScheduledFuture(hasRetryTimes, countDownLatch, scheduleExecutor,
                    conditionResult);
            boolean await = countDownLatch.await(timeout, timeUnit);
            scheduledFuture.cancel(true);
            predicateResult(hasRetryTimes, await, conditionResult);
        } catch (InterruptedException | TimeoutException e) {
            throw new CompletionException(e);
        } finally {
            scheduleExecutor.shutdownNow();
        }
    }

    private ScheduledFuture<?> getScheduledFuture(AtomicInteger hasRetryTimes, CountDownLatch countDownLatch,
            ScheduledThreadPoolExecutor scheduleExecutor, AtomicBoolean conditionResult) {
        return scheduleExecutor.scheduleWithFixedDelay(
                () -> {
                    if (hasRetryTimes.get() < maxRetryTimes) {
                        hasRetryTimes.incrementAndGet();
                        log.info("Await to check condition is matchable.");
                        if (until.get()) {
                            log.info("Successful await to condition is matched"
                                    + " after retried {} times.", hasRetryTimes.get());
                            conditionResult.compareAndSet(false, true);
                            countDownLatch.countDown();
                        }
                    } else {
                        log.warn("Failed to await match condition after retried {} times.", hasRetryTimes.get());
                        countDownLatch.countDown();
                    }
                },
                0, period, periodTimeUnit);
    }

    private void predicateResult(AtomicInteger hasRetryTimes, boolean await, AtomicBoolean conditionResult)
            throws TimeoutException {
        if (!await) {
            log.warn("Failed to await match condition after {} s.", timeout);
            throw new TimeoutException("Failed to await match condition after " + timeout + " s.");
        }
        if (!conditionResult.get() && hasRetryTimes.get() >= maxRetryTimes) {
            throw new TimeoutException(
                    "Failed to await match condition after retried " + hasRetryTimes.get() + " times.");
        }
    }

}
