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
package com.oceanbase.odc.service.worksheet.exceptions;

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.SAME_LEVEL_NUM_LIMIT;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;
import com.oceanbase.odc.core.shared.exception.NonTransient;

/**
 * Exceeding the limit number in the same level
 * 
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
public class ExceedSameLevelNumLimitException extends HttpException implements NonTransient {
    public ExceedSameLevelNumLimitException(String message) {
        this(message, null);
    }

    public ExceedSameLevelNumLimitException(String message, Throwable throwable) {
        this(new Object[] {SAME_LEVEL_NUM_LIMIT}, message, throwable);
    }

    public ExceedSameLevelNumLimitException(Object[] args, String message, Throwable throwable) {
        super(ErrorCodes.WorksheetExceedSameLevelNumLimit, args, message, throwable);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
