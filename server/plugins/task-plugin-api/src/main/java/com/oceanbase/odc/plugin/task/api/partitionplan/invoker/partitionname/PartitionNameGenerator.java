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
package com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname;

import java.sql.Connection;
import java.util.Map;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.PartitionPlanKeyInvoker;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link PartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-22 16:22
 * @since ODC_release_4.2.4
 */
public interface PartitionNameGenerator extends PartitionPlanKeyInvoker<String> {

    String TARGET_PARTITION_DEF_KEY = "targetPartition";

    String generate(@NonNull Connection connection, @NonNull String schema, @NonNull String tableName,
            @NonNull DBTablePartitionDefinition target, @NonNull Map<String, Object> parameters);

    @Override
    default String invoke(@NonNull Connection connection, @NonNull String schema,
            @NonNull String tableName, @NonNull Map<String, Object> parameters) {
        Object value = parameters.get(TARGET_PARTITION_DEF_KEY);
        if (!(value instanceof DBTablePartitionDefinition)) {
            throw new IllegalArgumentException("Missing target partition candidate");
        }
        return generate(connection, schema, tableName, (DBTablePartitionDefinition) value, parameters);
    }

}
