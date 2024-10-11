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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.Validate;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link RateLimitedExecutor}
 *
 * @author yh263208
 * @date 2024-10-11 16:21
 * @since ODC_release_4.3.2
 */
@Slf4j
public class RateLimitedExecutor implements Closeable {

    private final Lock queryQueueLock = new ReentrantLock();
    private final Condition queriesIsNotEmpty = this.queryQueueLock.newCondition();
    @Getter
    private final int qps;
    private final ThreadPoolExecutor asyncExecutor;
    private final Queue<Query<?>> queryQueue = new LinkedBlockingQueue<>();
    private List<Long> queryRecords = new ArrayList<>();
    @Setter
    @Getter
    private long submitTimeoutMillis = 120000;

    public RateLimitedExecutor(int qps, int consumerCount) {
        Validate.isTrue(qps > 0, "QPS can not be negative");
        Validate.isTrue(consumerCount > 0, "Concurrent can not be negative");
        this.qps = qps;
        this.asyncExecutor = new ThreadPoolExecutor(
                consumerCount, consumerCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new TaskThreadFactory("RateLimitedExecutor"), new ThreadPoolExecutor.AbortPolicy());
        for (int i = 0; i < consumerCount; i++) {
            this.asyncExecutor.submit(new QueryConsumer(this));
        }
    }

    public <T> T submit(Callable<T> query) throws Exception {
        boolean result = this.queryQueueLock.tryLock(this.submitTimeoutMillis, TimeUnit.MILLISECONDS);
        if (!result) {
            throw new TimeoutException("Failed to submit the query, reason: timeout");
        }
        Query<T> queryObject = new Query<>(query, this);
        try {
            this.queryQueue.add(queryObject);
            this.queriesIsNotEmpty.signalAll();
        } finally {
            this.queryQueueLock.unlock();
        }
        return queryObject.getResult();
    }

    private synchronized void recordQuery() {
        this.queryRecords.add(System.currentTimeMillis());
    }

    public synchronized int getQueryCountInLatestPeriod() {
        removeExpiredQueryRecords();
        return this.queryRecords.size();
    }

    public synchronized boolean isReadyForNextQuery() {
        return this.qps > getQueryCountInLatestPeriod();
    }

    private synchronized void removeExpiredQueryRecords() {
        long limit = System.currentTimeMillis() - 1000;
        int i = this.queryRecords.size() - 1;
        for (; i >= 0; i--) {
            if (this.queryRecords.get(i) <= limit) {
                break;
            }
        }
        if (i < 0) {
            return;
        } else if (i == this.queryRecords.size() - 1) {
            this.queryRecords = new ArrayList<>();
            return;
        }
        List<Long> newQueryRecords = new ArrayList<>(this.queryRecords.size() - i);
        for (int j = i; j < this.queryRecords.size(); j++) {
            newQueryRecords.add(this.queryRecords.get(j));
        }
        this.queryRecords = newQueryRecords;
    }

    @Override
    public void close() throws IOException {
        this.asyncExecutor.shutdownNow();
        log.info("RateLimitedExecutor has been shutdown");
    }

    private static class Query<T> {

        private final Callable<T> query;
        private final CountDownLatch latch;
        private final RateLimitedExecutor rateLimitedExecutor;
        private T result;
        private Exception thrown;

        public Query(Callable<T> query, RateLimitedExecutor rateLimitedExecutor) {
            this.query = query;
            this.latch = new CountDownLatch(1);
            this.rateLimitedExecutor = rateLimitedExecutor;
        }

        public void doQuery() {
            try {
                this.result = this.query.call();
            } catch (Exception e) {
                this.thrown = e;
            }
            this.latch.countDown();
        }

        public T getResult() throws Exception {
            boolean res = this.latch.await(this.rateLimitedExecutor.submitTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!res) {
                throw new TimeoutException("Failed to get result, reason: timeout");
            }
            if (this.thrown != null) {
                throw this.thrown;
            }
            return this.result;
        }
    }

    private static class TaskThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public TaskThreadFactory(@NonNull String poolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = poolName + "-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private static class QueryConsumer implements Runnable {

        private final RateLimitedExecutor rateLimitedExecutor;

        public QueryConsumer(@NonNull RateLimitedExecutor rateLimitedExecutor) {
            this.rateLimitedExecutor = rateLimitedExecutor;
        }

        @Override
        public void run() {
            log.warn("Query consumer thread has been started up...");
            while (!Thread.interrupted()) {
                try {
                    if (!this.rateLimitedExecutor.isReadyForNextQuery()) {
                        continue;
                    }
                    boolean res = this.rateLimitedExecutor.queryQueueLock.tryLock();
                    if (!res) {
                        continue;
                    }
                    Query<?> query;
                    try {
                        while (this.rateLimitedExecutor.queryQueue.isEmpty()) {
                            this.rateLimitedExecutor.queriesIsNotEmpty.await();
                        }
                        if (!this.rateLimitedExecutor.isReadyForNextQuery()) {
                            continue;
                        }
                        try {
                            this.rateLimitedExecutor.recordQuery();
                        } catch (Exception e) {
                            // eat the exp
                        }
                        query = this.rateLimitedExecutor.queryQueue.poll();
                    } finally {
                        this.rateLimitedExecutor.queryQueueLock.unlock();
                    }
                    query.doQuery();
                } catch (InterruptedException e) {
                    log.warn("Query Consumer thread will be exited, errMsg={}", e.getMessage());
                }
            }
            log.warn("Query Consumer thread will be exited");
        }
    }

}
