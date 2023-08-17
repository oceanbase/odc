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
package com.oceanbase.odc.service.iam;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.exception.AttemptLoginOverLimitException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

public class FailedLoginAttemptLimiterTest {

    @Test
    public void attempt_FirstSuccess_ReturnSuccess() {
        FailedLoginAttemptLimiter limiter = new FailedLoginAttemptLimiter(1, 1000L);

        Boolean result = limiter.attempt(() -> true);

        Assert.assertTrue(result);
    }

    @Test
    public void attempt_MaxTwoSecondTrue_ReturnTrue() {
        FailedLoginAttemptLimiter limiter = new FailedLoginAttemptLimiter(2, 1000L);

        limiter.attempt(() -> false);
        Boolean result = limiter.attempt(() -> true);

        Assert.assertTrue(result);
    }

    @Test
    public void attempt_MaxOneSecondTrueOverTimeout_ReturnTrue() throws InterruptedException {
        FailedLoginAttemptLimiter limiter = new FailedLoginAttemptLimiter(1, 1L);

        limiter.attempt(() -> false);
        TimeUnit.MILLISECONDS.sleep(2L);
        Boolean result = limiter.attempt(() -> true);

        Assert.assertTrue(result);
    }

    @Test(expected = AttemptLoginOverLimitException.class)
    public void attempt_MaxOneSecondTrue_ReturnFalse() {
        FailedLoginAttemptLimiter limiter = new FailedLoginAttemptLimiter(1, 1000L);

        limiter.attempt(() -> false);
        limiter.attempt(() -> true);
    }

    @Test(expected = BadRequestException.class)
    public void attempt_Exception_Exception() {
        FailedLoginAttemptLimiter limiter = new FailedLoginAttemptLimiter(1, 1000L);

        limiter.attempt(() -> {
            throw new BadRequestException("manual failed");
        });
    }

}
