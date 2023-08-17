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

import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

/**
 * @author yizhou.xw
 * @version : InternalServerError.java, v 0.1 2021-03-22 10:36
 */
public class InternalServerError extends HttpException implements Transient {

    public InternalServerError(String message) {
        this(ErrorCodes.InternalServerError, message);
    }

    public InternalServerError(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public InternalServerError(String message, Throwable throwable) {
        this(ErrorCodes.InternalServerError, message, throwable);
    }

    public InternalServerError(ErrorCode errorCode, String message, Throwable throwable) {
        super(errorCode, null, message, throwable);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
