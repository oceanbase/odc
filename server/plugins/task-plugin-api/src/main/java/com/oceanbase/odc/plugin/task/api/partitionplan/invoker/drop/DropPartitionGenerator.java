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
package com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.PartitionPlanKeyInvoker;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link DropPartitionGenerator}
 *
 * @author yh263208
 * @date 2024-01-19 17:41
 * @since ODC_release_4.2.4
 */
public interface DropPartitionGenerator extends PartitionPlanKeyInvoker<List<DBTablePartitionDefinition>> {

    String PARTITION_CANDIDATE_KEY = "candidates";

    List<DBTablePartitionDefinition> generate(@NonNull Connection connection, @NonNull String schema,
            @NonNull String tableName, @NonNull List<DBTablePartitionDefinition> candidates,
            @NonNull Map<String, Object> parameters);

    @Override
    default List<DBTablePartitionDefinition> invoke(@NonNull Connection connection, @NonNull String schema,
            @NonNull String tableName, @NonNull Map<String, Object> parameters) {
        Object definitions = parameters.get(PARTITION_CANDIDATE_KEY);
        if (!(definitions instanceof List)) {
            throw new IllegalArgumentException(PARTITION_CANDIDATE_KEY + " is missing");
        }
        return generate(connection, schema, tableName, (List<DBTablePartitionDefinition>) definitions, parameters);
    }

}
