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
 * @version : BadArgumentException.java, v 0.1 2021-03-19 20:24
 */
public class BadArgumentException extends HttpException implements NonTransient {
    public BadArgumentException(ErrorCode errorCode, String message) {
        super(errorCode, new Object[] {message}, message, null);
    }

    public BadArgumentException(String message, Throwable throwable) {
        super(ErrorCodes.BadArgument, new Object[] {message}, message, throwable);
    }

    public BadArgumentException(ErrorCode errorCode, Object[] args, String message) {
        super(errorCode, args, message);
    }

    public BadArgumentException(ErrorCode errorCode, Object[] args, String message, Throwable throwable) {
        super(errorCode, args, message, throwable);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
