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

import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link DateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-22 17:08
 * @since ODC_release_4.2.4
 */
public interface DateBasedPartitionNameGenerator extends PartitionNameGenerator {

    String PARTITION_NAME_GENERATOR_KEY = "partitionNameGeneratorConfig";

    String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull DBTablePartitionDefinition target, @NonNull DateBasedPartitionNameGeneratorConfig config);

    @Override
    default String getName() {
        return "DATE_BASED_PARTITION_NAME_GENERATOR";
    }

    @Override
    default String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull DBTablePartitionDefinition target, @NonNull Map<String, Object> parameters) {
        return generate(connection, dbTable, target, ParameterUtil.nullSafeExtract(parameters,
                PARTITION_NAME_GENERATOR_KEY, DateBasedPartitionNameGeneratorConfig.class));
    }

}
