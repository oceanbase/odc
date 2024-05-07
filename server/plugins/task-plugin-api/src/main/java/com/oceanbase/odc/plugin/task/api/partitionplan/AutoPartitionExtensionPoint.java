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
package com.oceanbase.odc.plugin.task.api.partitionplan;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link AutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2023-01-11 20:23
 * @since ODC_release_4.2.4
 * @see org.pf4j.ExtensionPoint
 */
public interface AutoPartitionExtensionPoint extends ExtensionPoint {

    List<DBTable> listAllPartitionedTables(@NonNull Connection connection,
            String tenantName, @NonNull String schemaName, List<String> tableNames);

    boolean supports(@NonNull DBTablePartition partition);

    String unquoteIdentifier(@NonNull String identifier);

    List<DataType> getPartitionKeyDataTypes(@NonNull Connection connection,
            @NonNull DBTable table) throws IOException, SQLException;

    List<String> generateCreatePartitionDdls(@NonNull Connection connection,
            @NonNull DBTablePartition partition);

    List<String> generateDropPartitionDdls(@NonNull Connection connection,
            @NonNull DBTablePartition partition, boolean reloadIndexes);

    DropPartitionGenerator getDropPartitionGeneratorByName(@NonNull String name);

    PartitionExprGenerator getPartitionExpressionGeneratorByName(@NonNull String name);

    PartitionNameGenerator getPartitionNameGeneratorGeneratorByName(@NonNull String name);

}
