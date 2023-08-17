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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test for {@link RetryExecutor}
 *
 * @author yh263208
 * @date 2022-05-20 18:00
 * @since ODC_release_3.3.1
 */
public class RetryExecutorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void run_illegalRetryTimes_expThrown() {
        RetryExecutor executor = RetryExecutor.builder().retryTimes(-1).build();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("RetryTimes is illegal -1");
        executor.run((Supplier<String>) () -> null, s -> false);
    }

    @Test
    public void run_run3Times_run3TimesSuccessfully() {
        RetryExecutor executor = RetryExecutor.builder().retryTimes(3).build();
        AtomicInteger counter = new AtomicInteger(0);
        executor.run((Supplier<String>) () -> {
            counter.incrementAndGet();
            return null;
        }, s -> false);
        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void run_run1TimesWith3RetryTimes_run1TimesSuccessfully() {
        RetryExecutor executor = RetryExecutor.builder().retryTimes(3).build();
        AtomicInteger counter = new AtomicInteger(0);
        executor.run((Supplier<String>) () -> {
            counter.incrementAndGet();
            return null;
        }, s -> true);
        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void run_intervalSet_delaySuccessfully() {
        RetryExecutor executor =
                RetryExecutor.builder().initialDelay(true).retryIntervalMillis(200).retryTimes(3).build();
        long start = System.currentTimeMillis();
        executor.run(() -> null, s -> false);
        Assert.assertTrue((System.currentTimeMillis() - start) > 600);
    }

    @Test
    public void run_initialDelay_delaySuccessfully() {
        RetryExecutor executor = RetryExecutor.builder().initialDelay(true).initialDelay(true).retryIntervalMillis(200)
                .retryTimes(3).build();
        long start = System.currentTimeMillis();
        executor.run(() -> null, s -> false);
        Assert.assertTrue((System.currentTimeMillis() - start) > 800);
    }
}
