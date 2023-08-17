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
package com.oceanbase.odc.core.shared.exception;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;

public class AttemptLoginOverLimitException extends HttpException implements Transient {
    public AttemptLoginOverLimitException(Double limit, Long tryAfterSeconds, String message) {
        super(ErrorCodes.AttemptLoginOverLimit,
                new Object[] {LimitMetric.FAILED_LOGIN_ATTEMPT_COUNT.getLocalizedMessage(), limit, tryAfterSeconds},
                message);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
