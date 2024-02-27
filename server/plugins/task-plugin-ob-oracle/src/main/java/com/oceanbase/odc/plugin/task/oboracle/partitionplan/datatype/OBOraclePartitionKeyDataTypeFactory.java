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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan.datatype;

import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.BasePartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.OBOracleAutoPartitionExtensionPoint;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBOraclePartitionKeyDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-26 14:03
 * @since ODC_release_4.2.4
 * @see BasePartitionKeyDataTypeFactory
 */
public class OBOraclePartitionKeyDataTypeFactory extends BasePartitionKeyDataTypeFactory {

    public OBOraclePartitionKeyDataTypeFactory(@NonNull SqlExprCalculator calculator,
            @NonNull DBTable dbTable, @NonNull String partitionKey) {
        super(calculator, dbTable, partitionKey);
    }

    @Override
    protected String unquoteIdentifier(String identifier) {
        return new OBOracleAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    @Override
    protected DataType recognizeColumnDataType(@NonNull DBTableColumn column) {
        return new OBOracleCommonDataTypeFactory(column.getTypeName()).generate();
    }

    /**
     * Oracle can only support column as a partition key, so that we don't need to do anything here
     */
    @Override
    protected DataType recognizeExprDataType(@NonNull DBTable dbTable, @NonNull String partitionKey) {
        return null;
    }

}
