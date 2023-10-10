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
package com.oceanbase.odc.service.onlineschemachange.exception;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.exception.HttpException;

import lombok.Getter;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Getter
public class OscException extends HttpException {

    protected String errorMessage;

    protected HttpStatus httpStatus;

    public OscException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public OscException(ErrorCode errorCode, String message,
            Throwable throwable) {
        super(errorCode, null, message, throwable);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.OK;
    }
}
