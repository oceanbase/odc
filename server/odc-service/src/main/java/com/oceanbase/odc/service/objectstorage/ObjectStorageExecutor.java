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
package com.oceanbase.odc.service.objectstorage;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/18 下午5:25
 * @Description: []
 */
@Slf4j
public class ObjectStorageExecutor {

    private final Semaphore semaphore;
    private final long tryLockTimeoutMilliseconds;

    public ObjectStorageExecutor(int concurrentLimit, long tryLockTimeoutMilliseconds) {
        this.semaphore = new Semaphore(concurrentLimit, true);
        this.tryLockTimeoutMilliseconds = tryLockTimeoutMilliseconds;
    }

    public <T> T concurrentSafeExecute(Supplier<T> supplier) {
        try {
            boolean acquired = semaphore.tryAcquire(tryLockTimeoutMilliseconds, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("Acquire storage executor timeout.");
                throw new InternalServerError("Acquire storage executor timeout");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            log.warn("Unexpected exception, ex={}", e);
            throw new UnexpectedException("concurrent execute failed");
        } finally {
            semaphore.release();
        }
    }

}
