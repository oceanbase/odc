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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link OBMySQLHistoricalPartitionPlanCreateGenerator} is a deprecated invoker, only for historic
 * partition plan data migration
 *
 * @author yh263208
 * @date 2024-03-11 16:05
 * @since ODC_release_4.2.4
 * @see OBMySQLTimeIncreasePartitionExprGenerator
 */
@Slf4j
@Deprecated
public class OBMySQLHistoricalPartitionPlanCreateGenerator extends OBMySQLTimeIncreasePartitionExprGenerator {

    private static final String TARGET_FUNCTION_NAME = "UNIX_TIMESTAMP";

    @Override
    public String getName() {
        return "HISTORICAL_PARTITION_PLAN_CREATE_GENERATOR";
    }

    @Override
    protected Date getPartitionUpperBound(@NonNull SqlExprCalculator calculator, @NonNull String partitionKey,
            @NonNull String upperBound) {
        if (StringUtils.startsWith(partitionKey, TARGET_FUNCTION_NAME)) {
            return new Date(Long.parseLong(upperBound) * 1000);
        }
        return super.getPartitionUpperBound(calculator, partitionKey, upperBound);
    }

    @Override
    public List<String> generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey, @NonNull Integer generateCount,
            @NonNull TimeIncreaseGeneratorConfig config) throws IOException, SQLException {
        if (StringUtils.startsWith(partitionKey, TARGET_FUNCTION_NAME)) {
            List<Date> candidates = getCandidateDates(connection, dbTable, partitionKey, config, generateCount);
            return candidates.stream().map(date -> date.getTime() / 1000 + "").collect(Collectors.toList());
        }
        return super.generate(connection, dbTable, partitionKey, generateCount, config);
    }

}
