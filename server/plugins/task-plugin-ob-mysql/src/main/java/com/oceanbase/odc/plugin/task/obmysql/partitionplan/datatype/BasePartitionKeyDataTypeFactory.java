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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

import lombok.NonNull;

/**
 * {@link BasePartitionKeyDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-23 15:21
 * @since ODC_release_4.2.4
 * @see DataTypeFactory
 */
public abstract class BasePartitionKeyDataTypeFactory implements DataTypeFactory {

    private final SqlExprCalculator calculator;
    private final DBTable dbTable;
    private final String partitionKey;

    public BasePartitionKeyDataTypeFactory(@NonNull SqlExprCalculator calculator,
            @NonNull DBTable dbTable, @NonNull String partitionKey) {
        this.calculator = calculator;
        this.dbTable = dbTable;
        this.partitionKey = partitionKey;
    }

    @Override
    public DataType generate() {
        DBTablePartitionOption option = this.dbTable.getPartition().getPartitionOption();
        if (option == null) {
            throw new IllegalStateException("Partition option is missing");
        }
        DBTablePartitionType partitionType = option.getType();
        if (partitionType == null || partitionType == DBTablePartitionType.UNKNOWN
                || partitionType == DBTablePartitionType.NOT_PARTITIONED) {
            throw new IllegalArgumentException("Partition type is illegal, " + partitionType);
        }
        String unquotedPartitionKey = unquoteIdentifier(this.partitionKey);
        /**
         * assume that the partition key is a column
         */
        Map<String, DBTableColumn> colName2Col = this.dbTable.getColumns().stream()
                .collect(Collectors.toMap(c -> unquoteIdentifier(c.getName()), c -> c));
        DBTableColumn column = colName2Col.get(unquotedPartitionKey);
        if (column != null) {
            return recognizeColumnDataType(column);
        }
        /**
         * assume that the partition key a an expression
         */
        DataType dataType = recognizeExprDataType(this.dbTable, this.partitionKey);
        if (dataType != null) {
            return dataType;
        }
        List<DBTablePartitionDefinition> definitions = this.dbTable.getPartition().getPartitionDefinitions();
        if (CollectionUtils.isEmpty(definitions)) {
            throw new IllegalStateException("Partition def is empty");
        }
        int i = getPartitionKeyIndex();
        DBTablePartitionDefinition definition = definitions.get(0);
        return this.calculator.calculate(definition.getMaxValues().get(i)).getDataType();
    }

    public int getPartitionKeyIndex() {
        DBTablePartitionOption option = this.dbTable.getPartition().getPartitionOption();
        if (option == null) {
            throw new IllegalStateException("Partition option is missing");
        }
        int i;
        List<DBTablePartitionDefinition> definitions = this.dbTable.getPartition().getPartitionDefinitions();
        if (CollectionUtils.isEmpty(definitions)) {
            throw new IllegalStateException("Partition def is empty");
        }
        List<String> cols = option.getColumnNames();
        if (CollectionUtils.isNotEmpty(cols)) {
            for (i = 0; i < cols.size(); i++) {
                if (Objects.equals(unquoteIdentifier(this.partitionKey), unquoteIdentifier(cols.get(i)))) {
                    break;
                }
            }
            if (i >= cols.size() || i >= definitions.size()) {
                throw new IllegalStateException("Failed to find partition key, " + this.partitionKey);
            }
        } else if (StringUtils.isNotEmpty(option.getExpression())
                && Objects.equals(unquoteIdentifier(option.getExpression()), unquoteIdentifier(this.partitionKey))) {
            i = 0;
        } else {
            throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
        }
        return i;
    }

    protected abstract String unquoteIdentifier(String identifier);

    protected abstract DataType recognizeColumnDataType(@NonNull DBTableColumn column);

    /**
     * recognize the data type of partition key by parsing.
     *
     * you can return null if you can not get it by parsing and then we will get the datatype by
     * executing it.
     *
     * @param partitionKey target partition key
     * @param dbTable target table
     * @return if null means that failed to get
     */
    protected abstract DataType recognizeExprDataType(@NonNull DBTable dbTable, @NonNull String partitionKey);

}
