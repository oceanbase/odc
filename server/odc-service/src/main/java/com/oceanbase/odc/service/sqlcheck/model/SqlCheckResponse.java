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
package com.oceanbase.odc.service.sqlcheck.model;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: ysj
 * @Date: 2025/3/12 09:57
 * @Since: 4.3.4
 * @Description:
 */
@Data
@Accessors(chain = true)
public class SqlCheckResponse<T> {

    private long affectedRows;
    private List<T> checkResults;

    public static <T> SqlCheckResponse<T> empty() {
        return new SqlCheckResponse<T>()
                .setAffectedRows(0L)
                .setCheckResults(new ArrayList<>());
    }

    public static <T> SqlCheckResponse<T> of(long affectedRows, List<T> checkResults) {
        return SqlCheckResponse.<T>empty()
                .setAffectedRows(ObjectUtil.defaultIfNull(affectedRows, 0L))
                .setCheckResults(ObjectUtil.defaultIfNull(checkResults, new ArrayList<>()));
    }
}
