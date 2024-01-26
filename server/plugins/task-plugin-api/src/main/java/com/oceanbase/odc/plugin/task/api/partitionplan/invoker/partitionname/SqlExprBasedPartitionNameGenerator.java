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

import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link SqlExprBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-22 17:02
 * @since ODC_release_4.2.2
 */
public interface SqlExprBasedPartitionNameGenerator extends PartitionNameGenerator {

    String PARTITION_NAME_GEN_CONFIG_KEY = "partitionNameGenerateConfig";

    String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull SqlExprBasedGeneratorConfig config) throws Exception;

    @Override
    default String getName() {
        return "CUSTOM_PARTITION_NAME_GENERATOR";
    }

    @Override
    default String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull Map<String, Object> parameters) throws Exception {
        return generate(connection, dbTable, targetPartitionIndex, target, ParameterUtil.nullSafeExtract(
                parameters, PARTITION_NAME_GEN_CONFIG_KEY, SqlExprBasedGeneratorConfig.class));
    }

}
