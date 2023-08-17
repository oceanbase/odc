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
package com.oceanbase.odc.service.dispatch;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;

import lombok.NonNull;

/**
 * Error for {@code RPC} response
 *
 * @author yh263208
 * @date 2022-03-28 19:42
 * @since ODC_release_3.3.0
 */
public class ErrorResponseException extends HttpException {

    private final HttpStatus httpStatus;
    private final String localizedMessage;

    public ErrorResponseException(HttpStatus httpStatus, @NonNull String errorCode, @NonNull String localizedMessage) {
        super(ErrorCodes.valueOf(errorCode), new Object[] {}, localizedMessage);
        this.httpStatus = httpStatus;
        this.localizedMessage = localizedMessage;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String getLocalizedMessage() {
        return this.localizedMessage;
    }
}
