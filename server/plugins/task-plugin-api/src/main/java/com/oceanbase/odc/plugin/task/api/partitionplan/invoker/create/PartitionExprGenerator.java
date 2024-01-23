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

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.PartitionPlanKeyInvoker;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.NonNull;

/**
 * {@link PartitionExprGenerator} is used to generate the value expression that can be written in
 * sql.
 *
 * @author yh263208
 * @date 2024-01-18 20:45
 * @since ODC_release_4.2.4
 */
public interface PartitionExprGenerator extends PartitionPlanKeyInvoker<List<String>> {

    String GENERATE_COUNT_KEY = "generateCount";
    String GENERATOR_PARAMETER_KEY = "generateParameter";
    String GENERATOR_PARTITION_KEY = "partitionKey";

    List<String> generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey, @NonNull Integer generateCount, @NonNull Map<String, Object> parameters);

    @Override
    default List<String> invoke(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Map<String, Object> parameters) {
        Integer generateCount = ParameterUtil.nullSafeExtract(parameters, GENERATE_COUNT_KEY, Integer.class);
        Validate.isTrue(generateCount > 0, "Partition generate count can not be smaller than 1");
        String partitionKey = ParameterUtil.nullSafeExtract(parameters, GENERATOR_PARTITION_KEY, String.class);
        return generate(connection, dbTable, partitionKey, generateCount, parameters);
    }

}
