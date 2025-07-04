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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.common.exception.LockNotObtainedException;

import lombok.extern.slf4j.Slf4j;

/**
 * TODO 目前的JdbcLockRegistry能力还比较弱，尤其是对于业务执行时间超过60s的场景存在风险，建议进行重构; 1、锁超时时间固定为60s，不支持按照lock维度设置锁超时时间
 * 2、不支持续锁
 */
@Slf4j
@Component
public class JdbcLockTemplate implements LockTemplate {

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    @Override
    public <T> T executeWithLock(String name, String key, long waitMillis,
            Supplier<T> supplier) {

        String lockKey = buildLockKey(name, key);
        Lock lock = jdbcLockRegistry.obtain(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockNotObtainedException();
            }
            log.debug("Acquired lock for key: {}", lockKey);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Lock acquisition was interrupted for key: " + lockKey, e);
        } finally {
            if (acquired) {
                lock.unlock();
                log.debug("Released lock for key: {}", lockKey);
            }
        }
    }

    @Override
    public void executeWithLock(String name, String key, long waitMillis, Runnable runnable) {
        executeWithLock(name, key, waitMillis, () -> {
            runnable.run();
            return null;
        });
    }

}
