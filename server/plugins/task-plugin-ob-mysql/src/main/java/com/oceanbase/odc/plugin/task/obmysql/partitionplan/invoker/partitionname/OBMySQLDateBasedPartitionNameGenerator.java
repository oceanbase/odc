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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname;

import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link OBMySQLDateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:01
 * @since ODC_release_4.2.4
 */
public class OBMySQLDateBasedPartitionNameGenerator implements DateBasedPartitionNameGenerator {

    @Override
    public String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull DateBasedPartitionNameGeneratorConfig config) {
        DateFormat dateFormat = new SimpleDateFormat(config.getNamingSuffixExpression());
        return config.getNamingPrefix() + dateFormat.format(new Date());
    }

}
