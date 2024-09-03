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
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link DateBasedPartitionNameGenerator} 由于 Range 分区通常为左闭右开，所以对于新增分区，以上一个分区的分区上界命名即可
 * 
 * @author yh263208
 * @date 2024-01-22 17:08
 * @since ODC_release_4.2.4
 */
public interface DateBasedPartitionNameGenerator extends PartitionNameGenerator {

    String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull DateBasedPartitionNameGeneratorConfig config, @NonNull List<String> base) throws Exception;

    @Override
    default String getName() {
        return "DATE_BASED_PARTITION_NAME_GENERATOR";
    }

    @Override
    default String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull Map<String, Object> parameters) throws Exception {
        return generate(connection, dbTable, targetPartitionIndex, target, ParameterUtil.nullSafeExtract(
                parameters, PARTITION_NAME_GENERATOR_KEY, DateBasedPartitionNameGeneratorConfig.class),
                ParameterUtil.nullSafeExtract(parameters, PREVIOUS_PARTITION_EXPRS,
                        List.class));
    }

}
