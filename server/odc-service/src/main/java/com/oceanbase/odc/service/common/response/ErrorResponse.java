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
package com.oceanbase.odc.service.common.response;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author yizhou.xw
 * @version : ErrorResponse.java, v 0.1 2021-02-19 14:01
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse extends BaseResponse {
    private Error error;

    @JsonInclude(Include.NON_ABSENT)
    private String code;

    @JsonInclude(Include.NON_ABSENT)
    private String message;

    public ErrorResponse(HttpStatus httpStatus, Error error) {
        super.setSuccessful(false);
        super.setHttpStatus(httpStatus);
        this.setError(error);
    }

    private void setError(Error error) {
        this.error = error;
        this.code = error.getCode();
        this.message = error.getMessage();
    }
}
