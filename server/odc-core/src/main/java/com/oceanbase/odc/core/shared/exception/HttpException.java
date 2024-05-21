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

import com.oceanbase.odc.core.shared.constant.ErrorCode;

import lombok.Getter;

/**
 * @author yizhou.xw
 * @version : HttpException.java, v 0.1 2021-02-19 18:08
 */
@Getter
public abstract class HttpException extends OdcException implements HttpError {

    /**
     * args for render error message
     */
    private final Object[] args;

    public HttpException(ErrorCode errorCode, Object[] args, String message) {
        this(errorCode, args, message, null);
    }

    public HttpException(ErrorCode errorCode, Object[] args, String message, Throwable throwable) {
        super(errorCode, message, throwable);
        this.args = args;
    }

    @Override
    public String getLocalizedMessage() {
        return getErrorCode().getLocalizedMessage(args);
    }

    public String getEnglishMessage() {
        return getErrorCode().getEnglishMessage(args);
    }

}
