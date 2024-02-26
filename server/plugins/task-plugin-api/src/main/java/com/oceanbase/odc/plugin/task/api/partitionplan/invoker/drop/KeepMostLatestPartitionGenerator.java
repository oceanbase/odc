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

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link KeepMostLatestPartitionGenerator}
 *
 * @author yh263208
 * @date 2024-01-19 17:49
 * @since ODC_release_4.2.4
 */
public interface KeepMostLatestPartitionGenerator extends DropPartitionGenerator {

    String KEEP_LATEST_COUNT_KEY = "keepLatestCount";
    String RELOAD_INDEXES = "reloadIndexes";

    List<DBTablePartitionDefinition> generate(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull Integer keepCount) throws Exception;

    @Override
    default String getName() {
        return "KEEP_MOST_LATEST_GENERATOR";
    }

    @Override
    default List<DBTablePartitionDefinition> invoke(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull Map<String, Object> parameters) throws Exception {
        Integer keepCount = ParameterUtil.nullSafeExtract(parameters, KEEP_LATEST_COUNT_KEY, Integer.class);
        Validate.isTrue(keepCount > 0, "Keep count can not be smaller than 1");
        return generate(connection, dbTable, keepCount);
    }

}
