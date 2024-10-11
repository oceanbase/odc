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
package com.oceanbase.odc.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link RateLimitedExecutor}
 *
 * @author yh263208
 * @date 2024-10-11 19:25
 * @since ODC_release_4.3.2
 */
public class RateLimitedExecutorTest {

    @Test
    public void submit_simpleTask_executedSucceed() throws Exception {
        try (RateLimitedExecutor executor = new RateLimitedExecutor(1, 1)) {
            Assert.assertEquals(0, executor.getQueryCountInLatestPeriod());
            String actual = executor.submit(() -> "Hello,world");
            Assert.assertEquals("Hello,world", actual);
            Assert.assertEquals(1, executor.getQueryCountInLatestPeriod());
        }
    }

    @Test
    public void submit_qps3AndSubmit10QuerySecond_executedSucceed() throws Exception {
        List<Long> timestamps = new ArrayList<>();
        try (RateLimitedExecutor executor = new RateLimitedExecutor(3, 1)) {
            List<Thread> threads = new ArrayList<>();
            submitTask(executor, timestamps, threads, System.currentTimeMillis(), 10);
            joinThreads(threads);
        }
        Assert.assertEquals(10, timestamps.size());
        assertQpsEquals(3, timestamps);
    }

    @Test(expected = TimeoutException.class)
    public void submit_submitLongTaskAndSetTimeoutShort_timeoutExpThrown() throws Exception {
        try (RateLimitedExecutor executor = new RateLimitedExecutor(1, 1)) {
            executor.setSubmitTimeoutMillis(100);
            Thread t = new Thread(() -> {
                try {
                    executor.submit(() -> {
                        Thread.sleep(20000);
                        return null;
                    });
                } catch (Exception ignored) {

                }
            });
            t.start();
            Thread.sleep(100);
            executor.submit(() -> null);
        }
    }

    @Test
    public void submit_qps3AndSubmit10QuerySecond5Consumer_executedSucceed() throws Exception {
        List<Long> timestamps = new ArrayList<>();
        try (RateLimitedExecutor executor = new RateLimitedExecutor(3, 5)) {
            List<Thread> threads = new ArrayList<>();
            submitTask(executor, timestamps, threads, System.currentTimeMillis(), 10);
            joinThreads(threads);
        }
        Assert.assertEquals(10, timestamps.size());
        assertQpsEquals(3, timestamps);
    }

    @Test
    public void submit_qps1AndSubmit7QueryUsing3Seconds5Consumer_executedSucceed() throws Exception {
        List<Long> timestamps = new ArrayList<>();
        try (RateLimitedExecutor executor = new RateLimitedExecutor(1, 5)) {
            List<Thread> threads = new ArrayList<>();
            long start = System.currentTimeMillis();
            submitTask(executor, timestamps, threads, start, 2);
            Thread.sleep(1123);
            submitTask(executor, timestamps, threads, start, 3);
            Thread.sleep(345);
            submitTask(executor, timestamps, threads, start, 2);
            joinThreads(threads);
        }
        Assert.assertEquals(7, timestamps.size());
        assertQpsEquals(1, timestamps);
    }

    private void assertQpsEquals(int expectQps, List<Long> timestamps) {
        int realQps = -1;
        for (int i = 0; i < timestamps.size(); i++) {
            long current = timestamps.get(i);
            long limit = current + 1000;
            int j = i;
            int realQpsItem = 0;
            for (; j < timestamps.size(); j++) {
                if (timestamps.get(j) >= limit) {
                    break;
                }
                realQpsItem++;
            }
            realQps = Math.max(realQpsItem, realQps);
        }
        Assert.assertTrue(realQps <= expectQps + 1 && realQps >= expectQps);
    }

    private void submitTask(RateLimitedExecutor executor,
            List<Long> timestamps, List<Thread> threads, long start, int count) {
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                try {
                    executor.submit((Callable<Void>) () -> {
                        synchronized (timestamps) {
                            timestamps.add(System.currentTimeMillis() - start);
                        }
                        return null;
                    });
                } catch (Exception e) {
                    // eat the exp
                }
            });
            thread.start();
            threads.add(thread);
        }
    }

    private void joinThreads(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

}
