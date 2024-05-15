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
 * @version : AccessDeniedException.java, v 0.1 2021-02-19 18:27
 */
public class AccessDeniedException extends HttpException implements NonTransient {
    public AccessDeniedException() {
        this("");
    }

    public AccessDeniedException(String message) {
        this(ErrorCodes.AccessDenied, message);
    }

    public AccessDeniedException(ErrorCode errorCode, String message) {
        super(errorCode, new Object[] {message}, message);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
