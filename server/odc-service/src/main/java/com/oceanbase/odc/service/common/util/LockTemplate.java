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
package com.oceanbase.odc.service.common.util;

import java.util.function.Supplier;

public interface LockTemplate {

    /**
     * Default lock expiration time in seconds
     */
    int DEFAULT_LOCK_EXPIRE_SECONDS = 60;

    /**
     * Default wait time for acquiring lock in milliseconds
     */
    long DEFAULT_WAIT_MILLIS = 5000;

    /**
     * Build lock key from name and key
     *
     * @param name lock name
     * @param key lock key (can be null)
     * @return combined lock key
     */
    default String buildLockKey(String name, String key) {
        if (key == null || key.trim().isEmpty()) {
            return name;
        }
        return name + "-" + key;
    }

    /**
     * Execute with lock using default parameters, throw exception if lock cannot be acquired
     * 
     * @param name lock name
     * @param key lock key (can be null or other unique ID)
     * @param supplier the operation to execute
     * @param <T> return type
     * @return result of the operation
     */
    default <T> T executeWithLock(String name, String key, Supplier<T> supplier) {
        return executeWithLock(name, key, DEFAULT_WAIT_MILLIS, supplier);
    }

    /**
     * Execute with lock for void operations using default parameters, throw exception if lock cannot be
     * acquired
     * 
     * @param name lock name
     * @param key lock key (can be null or other unique ID)
     * @param runnable the operation to execute
     */
    default void executeWithLock(String name, String key, Runnable runnable) {
        executeWithLock(name, key, DEFAULT_WAIT_MILLIS, runnable);
    }

    /**
     * Execute with lock, throw exception if lock cannot be acquired
     * 
     * @param name lock name
     * @param key lock key (can be null or other unique ID)
     * @param waitMillis wait time for acquiring lock in milliseconds
     * @param supplier the operation to execute
     * @param <T> return type
     * @return result of the operation
     */
    <T> T executeWithLock(String name, String key, long waitMillis, Supplier<T> supplier);

    /**
     * Execute with lock for void operations, throw exception if lock cannot be acquired
     * 
     * @param name lock name
     * @param key lock key (can be null or other unique ID)
     * @param waitMillis wait time for acquiring lock in milliseconds
     * @param runnable the operation to execute
     */
    void executeWithLock(String name, String key, long waitMillis, Runnable runnable);

}
