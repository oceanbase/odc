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
package com.oceanbase.odc.service.iam.util;

import java.util.function.Supplier;

import com.oceanbase.odc.core.shared.exception.AttemptLoginOverLimitException;

import lombok.extern.slf4j.Slf4j;

/**
 * simple failed login attempt limiter, restrict failed login attempt too much times from same
 * client address
 * 
 * @author yizhou.xw
 * @version : FailedLoginAttemptLimiter.java, v 0.1 2021-05-31 14:49
 */
@Slf4j
public class FailedLoginAttemptLimiter {

    private final int maxFailedAttempt;
    private final long lockTimeoutMillis;

    public FailedLoginAttemptLimiter(int maxFailedAttempt, long lockTimeoutMillis) {
        this.maxFailedAttempt = maxFailedAttempt <= 0 ? Integer.MAX_VALUE : maxFailedAttempt;
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    private volatile int failedAttempt = 0;
    private volatile boolean isLocked = false;
    private volatile long lastLockedMills = 0;

    public int getRemainAttempt() {
        if (isLocked) {
            return 0;
        } else if (this.maxFailedAttempt == Integer.MAX_VALUE) {
            return -1;
        }
        return Math.max(0, maxFailedAttempt - failedAttempt);
    }

    public synchronized void reduceFailedAttemptCount() {
        if (this.failedAttempt > 0) {
            this.failedAttempt--;
        }
    }

    /**
     * 尝试执行某个操作，如果操作失败则进行限流
     *
     * @param attemptResultSupplier 尝试执行的操作
     * @return 操作结果
     * @throws AttemptLoginOverLimitException 当连续失败次数达到限制时抛出异常
     */
    public synchronized Boolean attempt(Supplier<Boolean> attemptResultSupplier) {
        long currentTimeMillis = System.currentTimeMillis();
        // 如果已经被限流且距离上次限流时间超过了限制时间或者限制时间为0，则重新初始化限流状态
        if (isLocked && (currentTimeMillis > lastLockedMills + lockTimeoutMillis || lockTimeoutMillis <= 0)) {
            isLocked = false;
            failedAttempt = 0;
        }
        if (isLocked) {
            // 计算距离上次限流时间的剩余时间（秒）
            long remainSeconds = (lastLockedMills + lockTimeoutMillis - currentTimeMillis) / 1000L;
            // 抛出连续失败次数达到限制的异常
            throw new AttemptLoginOverLimitException((double) maxFailedAttempt, remainSeconds,
                String.format("failed attempt over limit, failedAttempt=%d, limit=%d, remainSeconds=%d",
                    failedAttempt, maxFailedAttempt, remainSeconds));
        }
        Boolean result = null;
        try {
            // 执行尝试操作
            result = attemptResultSupplier.get();
            return result;
        } finally {
            if (result == null || !result) {
                // 操作失败，记录失败次数并判断是否需要限流
                log.info("attempt failed, currentFailedAttempt={}", failedAttempt);
                failedAttempt++;
                if (failedAttempt >= maxFailedAttempt) {
                    isLocked = true;
                    lastLockedMills = currentTimeMillis;
                }
            }
        }
    }

}
