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

import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.commons.collections4.CollectionUtils;

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

    private volatile int failedAttempt = 0;
    private volatile boolean isLocked = false;
    private volatile long lastLockedMills = 0;

    public FailedLoginAttemptLimiter(int maxFailedAttempt, long lockTimeoutMillis) {
        this.maxFailedAttempt = maxFailedAttempt <= 0 ? Integer.MAX_VALUE : maxFailedAttempt;
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    public int getRemainAttempt() {
        if (isLocked) {
            return 0;
        } else if (this.maxFailedAttempt == Integer.MAX_VALUE) {
            return -1;
        }
        return Math.max(0, maxFailedAttempt - failedAttempt);
    }


    public synchronized Boolean attempt(BooleanSupplier attemptResultSupplier) {
        long currentTimeMillis = System.currentTimeMillis();
        if (isLocked && (currentTimeMillis > lastLockedMills + lockTimeoutMillis || lockTimeoutMillis <= 0)) {
            isLocked = false;
            failedAttempt = 0;
        }
        if (isLocked) {
            long remainSeconds = (lastLockedMills + lockTimeoutMillis - currentTimeMillis) / 1000L;
            throw new AttemptLoginOverLimitException((double) maxFailedAttempt, remainSeconds,
                    String.format("failed attempt over limit, failedAttempt=%d, limit=%d, remainSeconds=%d",
                            failedAttempt, maxFailedAttempt, remainSeconds));
        }
        Boolean result = null;
        try {
            result = attemptResultSupplier.getAsBoolean();
            return result;
        } finally {
            if (result == null || !result) {
                log.info("attempt failed, currentFailedAttempt={}", failedAttempt);
                failedAttempt++;
                if (failedAttempt >= maxFailedAttempt) {
                    isLocked = true;
                    lastLockedMills = currentTimeMillis;
                }
            }
        }
    }


    public synchronized void attemptFailedByException(Runnable runnable,
            List<Class<? extends Throwable>> attemptFailedException) {
        long currentTimeMillis = System.currentTimeMillis();
        if (isLocked && (currentTimeMillis > lastLockedMills + lockTimeoutMillis || lockTimeoutMillis <= 0)) {
            isLocked = false;
            failedAttempt = 0;
        }
        if (isLocked) {
            long remainSeconds = (lastLockedMills + lockTimeoutMillis - currentTimeMillis) / 1000L;
            throw new AttemptLoginOverLimitException((double) maxFailedAttempt, remainSeconds,
                    String.format("failed attempt over limit, failedAttempt=%d, limit=%d, remainSeconds=%d",
                            failedAttempt, maxFailedAttempt, remainSeconds));
        }
        try {
            runnable.run();
        } catch (Exception e) {
            if (isAttemptFailed(e, attemptFailedException)) {
                log.info("attempt failed, currentFailedAttempt={}", failedAttempt);
                failedAttempt++;
                if (failedAttempt >= maxFailedAttempt) {
                    isLocked = true;
                    lastLockedMills = currentTimeMillis;
                }
            }
            throw e;
        }
    }


    private boolean isAttemptFailed(Exception cause, List<Class<? extends Throwable>> attemptFailedException) {
        if (CollectionUtils.isEmpty(attemptFailedException)) {
            return false;
        }
        for (Class<? extends Throwable> e : attemptFailedException) {
            if (e != null && cause.getClass().isAssignableFrom(e)) {
                return true;
            }
        }
        return false;
    }

}
