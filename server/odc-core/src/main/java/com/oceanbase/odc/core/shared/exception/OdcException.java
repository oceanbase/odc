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
 * base exception type definition to indicate it is a ODC exception
 * 
 * @author yizhou.xw
 * @version : OdcException.java, v 0.1 2021-02-19 17:52
 */
public abstract class OdcException extends RuntimeException {

    @Getter
    private final ErrorCode errorCode;

    public OdcException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    OdcException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    OdcException(ErrorCode errorCode, String message, Throwable throwable) {
        super(message, throwable);
        this.errorCode = errorCode;
    }
}
