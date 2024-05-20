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

import lombok.NoArgsConstructor;

/**
 * 分页 response
 *
 * <pre>
 * avoid add below annotation due openapi-ui bug
 * &#64;Schema(name = "PaginatedResponse", description = "分页结果 response")
 * </pre>
 *
 * @param <T>
 *
 */
@NoArgsConstructor
public class PaginatedResponse<T> extends SuccessResponse<PaginatedData<T>> {

    public PaginatedResponse(PaginatedData<T> data) {
        super(data);
    }

}
