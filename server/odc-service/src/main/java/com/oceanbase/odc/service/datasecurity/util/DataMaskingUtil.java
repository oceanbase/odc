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
package com.oceanbase.odc.service.datasecurity.util;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.datasecurity.extractor.model.DBColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;

/**
 * @author gaoda.xy
 * @date 2023/7/3 17:15
 */
public class DataMaskingUtil {

    public static boolean isSensitiveColumnExists(List<Set<SensitiveColumn>> columns) {
        if (columns.isEmpty()) {
            return false;
        }
        for (Set<SensitiveColumn> columnSet : columns) {
            if (CollectionUtils.isNotEmpty(columnSet)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDBColumnExists(List<Set<DBColumn>> columns) {
        if (columns.isEmpty()) {
            return false;
        }
        for (Set<DBColumn> columnSet : columns) {
            if (CollectionUtils.isNotEmpty(columnSet)) {
                return true;
            }
        }
        return false;
    }

}
