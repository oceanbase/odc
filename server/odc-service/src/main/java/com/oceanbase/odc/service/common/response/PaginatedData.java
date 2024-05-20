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

import java.util.List;

import com.google.common.collect.FluentIterable;
import com.oceanbase.odc.service.common.model.Stats;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页数据，包含 列表 和 分页信息 <br>
 * 相对于 {@link ListData}，增加了 page 信息
 *
 * @param <T>
 */
@Data
@NoArgsConstructor
public class PaginatedData<T> {

    private CustomPage page;

    /**
     * use List not Iterable due swagger ui does not support Iterable type
     */
    private List<T> contents;

    private Stats stats;

    public PaginatedData(Iterable<T> contents, CustomPage page) {
        this.contents = FluentIterable.from(contents).toList();
        this.page = page;
        this.page.setNumber(this.page.getNumber());
    }

}
