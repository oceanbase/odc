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
package com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.plugin.task.api.partitionplan.model.NumberIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.NonNull;

public interface NumberIncreasePartitionExprGenerator extends PartitionExprGenerator {

    String GENERATOR_NAME = "NUMBER_INCREASING_GENERATOR";

    List<String> generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey, @NonNull Integer generateCount,
            @NonNull NumberIncreaseGeneratorConfig config) throws Exception;

    @Override
    default String getName() {
        return GENERATOR_NAME;
    }

    @Override
    default List<String> generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey, @NonNull Integer generateCount,
            @NonNull Map<String, Object> parameters) throws Exception {
        return generate(connection, dbTable, partitionKey, generateCount,
                ParameterUtil.nullSafeExtract(parameters, GENERATOR_PARAMETER_KEY,
                        NumberIncreaseGeneratorConfig.class));
    }

}
