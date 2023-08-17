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

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.oceanbase.odc.service.common.model.Stats;

import lombok.Data;

/**
 * 包含统计信息的分页结果
 * 
 * @param <T>
 */
@Data
public class PageAndStats<T> {

    private Page<T> page;

    @JsonInclude(Include.NON_NULL)
    private Stats stats;

    public static <T> PageAndStats<T> of(Page<T> page, Stats stats) {
        PageAndStats<T> ret = new PageAndStats<>();
        ret.setPage(page);
        ret.setStats(stats);
        return ret;
    }

    public static <T> PageAndStats<T> empty() {
        PageAndStats<T> ret = new PageAndStats<>();
        ret.setPage(Page.empty());
        ret.setStats(new Stats());
        return ret;
    }

}
