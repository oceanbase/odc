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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.FluentIterable;
import com.oceanbase.odc.service.common.model.Stats;

import lombok.Getter;
import lombok.Setter;

/**
 * 列表数据
 * 
 * @param <T>
 */
@Getter
@Setter
public class ListData<T> {
    private List<T> contents;

    @JsonInclude(Include.NON_NULL)
    private Stats stats;

    public ListData() {}

    ListData(Iterable<T> contents) {
        this.contents = FluentIterable.from(contents).toList();
    }
}
