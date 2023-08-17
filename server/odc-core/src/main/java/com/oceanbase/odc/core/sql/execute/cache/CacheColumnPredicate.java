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
package com.oceanbase.odc.core.sql.execute.cache;

import java.sql.ResultSetMetaData;
import java.util.function.BiPredicate;

import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache column predicate, according to the predicate to determine which columns need to be cached
 * and which do not
 *
 * @author yh263208
 * @date 2021-11-04 11:15
 * @since ODC_release_3.2.2
 */
@Slf4j
public class CacheColumnPredicate implements BiPredicate<Integer, ResultSetMetaData> {

    @Override
    public boolean test(Integer index, ResultSetMetaData metaData) {
        try {
            return DataTypeUtil.isBinaryType(metaData.getColumnTypeName(index + 1));
        } catch (Exception e) {
            log.warn("Failed to get data type", e);
        }
        return false;
    }

}

