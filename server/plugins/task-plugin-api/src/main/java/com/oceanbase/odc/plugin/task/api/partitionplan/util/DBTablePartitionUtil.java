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
package com.oceanbase.odc.plugin.task.api.partitionplan.util;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;

import lombok.NonNull;

/**
 * {@link DBTablePartitionUtil}
 *
 * @author yh263208
 * @date 2024-03-22 10:12
 * @since ODC_release_4.2.4
 */
public class DBTablePartitionUtil {

    public static int getPartitionKeyIndex(@NonNull DBTable dbTable, @NonNull String partitionKey,
            @NonNull Function<String, String> unquoteMapper) {
        DBTablePartitionOption option = dbTable.getPartition().getPartitionOption();
        if (option == null) {
            throw new IllegalStateException("Partition option is missing");
        }
        int i;
        List<DBTablePartitionDefinition> definitions = dbTable.getPartition().getPartitionDefinitions();
        if (CollectionUtils.isEmpty(definitions)) {
            throw new IllegalStateException("Partition def is empty");
        }
        List<String> cols = option.getColumnNames();
        if (CollectionUtils.isNotEmpty(cols)) {
            for (i = 0; i < cols.size(); i++) {
                if (Objects.equals(unquoteMapper.apply(partitionKey), unquoteMapper.apply(cols.get(i)))) {
                    break;
                }
            }
            if (i >= cols.size() || i >= definitions.size()) {
                throw new IllegalStateException("Failed to find partition key, " + partitionKey);
            }
        } else if (StringUtils.isNotEmpty(option.getExpression())
                && Objects.equals(unquoteMapper.apply(option.getExpression()), unquoteMapper.apply(partitionKey))) {
            i = 0;
        } else {
            throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
        }
        return i;
    }

}
