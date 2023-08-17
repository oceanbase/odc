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
package com.oceanbase.odc.service.objectstorage.cloud;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;
import com.oceanbase.odc.core.shared.exception.NonTransient;

public class InvalidFileFormatException extends HttpException implements NonTransient {
    public InvalidFileFormatException() {
        this("");
    }

    public InvalidFileFormatException(String message) {
        this(ErrorCodes.InvalidFileFormat, message);
    }

    public InvalidFileFormatException(ErrorCode errorCode, String message) {
        super(errorCode, new Object[] {message}, message);
    }

    @Override
    public HttpStatus httpStatus() {
        return BAD_REQUEST;
    }
}
