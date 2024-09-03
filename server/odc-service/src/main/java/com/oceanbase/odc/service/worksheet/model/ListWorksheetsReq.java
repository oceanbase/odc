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
package com.oceanbase.odc.service.worksheet.model;

import javax.validation.constraints.Min;

import lombok.Data;

/**
 * list worksheet request
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Data
public class ListWorksheetsReq {
    /**
     * retrieve sub-worksheets from the specified {@link this#path}.
     * <p>
     * if path is null,will list worksheets of root directory.
     */
    private String path;
    /**
     * retrieve sub-worksheets from the specified {@link this#path} at varying {@link this#depth}
     * levels. 0: all sub-levels, 1: first sub-level, 2: first + second sub-level, and so on.
     * <p>
     * if depth is null,will set default value 1
     */
    @Min(0)
    private Integer depth;
    /**
     * filter list worksheets by fuzzy matching worksheet name.If nameLike is null,do not perform
     * filtering operation.
     */
    private String nameLike;
}
