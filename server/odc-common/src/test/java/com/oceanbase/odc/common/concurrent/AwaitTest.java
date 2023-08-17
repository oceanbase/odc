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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author yaobin
 * @date 2023-08-09
 * @since 4.2.0
 */
public class AwaitTest {
    @Test
    public void test_until_condition_successful() {
        AtomicInteger ai = new AtomicInteger();
        Await.await().until(() -> {
            for (;;) {
                if (ai.incrementAndGet() > 5) {
                    break;
                }
            }
            return true;
        }).timeout(1).timeUnit(TimeUnit.SECONDS).build().start();
        Assert.assertEquals(6, ai.get());
    }

    @Test(expected = CompletionException.class)
    public void test_until_timeout_failed() {
        AtomicInteger ai = new AtomicInteger();
        Await.await().until(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        }).timeout(100).timeUnit(TimeUnit.MILLISECONDS).build().start();
        Assert.assertEquals(6, ai.get());
    }

    @Test(expected = CompletionException.class)
    public void test_with_throw_exception_timeout_failed() {
        Await.await().timeout(3).until(() -> {
            throw new IllegalArgumentException("Bad argument");
        }).build().start();
    }

    @Test
    public void test_until_retry_time_successful() {
        AtomicInteger ai = new AtomicInteger();
        Await.await().until(() -> ai.incrementAndGet() >= 2)
                .maxRetryTimes(2).timeout(30).timeUnit(TimeUnit.SECONDS).build().start();
        Assert.assertEquals(2, ai.get());
    }

    @Test(expected = CompletionException.class)
    public void test_until_exceed_retry_time_failed() {
        AtomicInteger ai = new AtomicInteger();
        Await.await().until(() -> ai.incrementAndGet() >= 3)
                .maxRetryTimes(2).timeout(30).timeUnit(TimeUnit.SECONDS).build().start();
    }

    @Test
    public void test_without_set_until_successful() throws Exception {
        CompletableFuture<Void> completableFuture =
                CompletableFuture.runAsync(() -> Await.await()
                        .threadName("test-thread-%d").timeout(1).build().start());
        Void res = completableFuture.get(6, TimeUnit.SECONDS);
        Assert.assertNull(res);
    }

}
