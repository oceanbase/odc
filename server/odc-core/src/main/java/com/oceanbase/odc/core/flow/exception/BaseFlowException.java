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
package com.oceanbase.odc.core.flow.exception;

import com.oceanbase.odc.core.shared.constant.ErrorCode;

import lombok.Getter;

/**
 * exception, all exceptions need to be thrown by this class
 *
 * @author yh263208
 * @date 2022-03-01 13:42
 * @since ODC_release_3.3.0
 */
public abstract class BaseFlowException extends RuntimeException {
    /**
     * args for render error message
     */
    private final Object[] args;
    @Getter
    private final ErrorCode errorCode;

    public BaseFlowException(ErrorCode errorCode, Object[] args, String message) {
        this(errorCode, args, message, null);
    }

    public BaseFlowException(ErrorCode errorCode, Object[] args, String message, Throwable throwable) {
        super(message, throwable);
        this.args = args;
        this.errorCode = errorCode;
    }

    @Override
    public String getLocalizedMessage() {
        return getErrorCode().getLocalizedMessage(args);
    }

}
