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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.ToString.Exclude;

/**
 * @author yizhou.xw
 * @version : SuccessResponse.java, v 0.1 2021-02-19 14:58
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SuccessResponse<T> extends BaseResponse {

    /**
     * result value，may object/array
     */
    @Exclude
    private T data;

    public SuccessResponse() {
        this(null);
    }

    public SuccessResponse(T value) {
        super.setSuccessful(true);
        super.setHttpStatus(HttpStatus.OK);
        this.data = value;
    }
}
