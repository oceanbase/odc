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

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
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
        if (StringUtils.isNotEmpty(option.getExpression())) {
            /**
             * range partition, partition key a an expression
             */
            List<DBTablePartitionDefinition> definitions = this.dbTable.getPartition().getPartitionDefinitions();
            if (CollectionUtils.isEmpty(definitions)) {
                throw new IllegalStateException("Partition def is empty");
            }
            DBTablePartitionDefinition definition = definitions.get(0);
            /**
             * ob-oracle and ob-mysql only has one partition key for range partition type, but is that true for
             * other datasource?
             */
            Verify.singleton(definition.getMaxValues(), "Range partition table can only has one partition key");
            DataType dataType = recognizeExprDataType(this.partitionKey);
            if (dataType != null) {
                return dataType;
            }
            SqlExprResult result = this.calculator.calculate(definition.getMaxValues().get(0));
            Verify.notNull(result, "Sql expression's calculation result can not be null");
            return result.getDataType();
        } else if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            /**
             * range column partition, partition key is a column
             */
            Map<String, DBTableColumn> colName2Col = this.dbTable.getColumns().stream()
                    .collect(Collectors.toMap(c -> unquoteIdentifier(c.getName()), c -> c));
            List<String> cols = option.getColumnNames();
            String pk = unquoteIdentifier(partitionKey);
            if (cols.stream().noneMatch(s -> Objects.equals(pk, unquoteIdentifier(s)))) {
                throw new IllegalArgumentException("Failed to find " + pk + " in partition keys " + cols);
            }
            DBTableColumn column = colName2Col.get(pk);
            if (column == null) {
                throw new IllegalStateException("Failed to find target column by name, " + pk);
            }
            return recognizeColumnDataType(column);
        }
        throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
    }

    protected abstract String unquoteIdentifier(String identifier);

    protected abstract DataType recognizeColumnDataType(@NonNull DBTableColumn column);

    /**
     * recognize the data type of partition key by parsing.
     *
     * you can return null if you can not get it by parsing and then we will get the datatype by
     * executing it.
     *
     * @param partitionKeyExpression target partition key
     * @return if null means that failed to get
     */
    protected abstract DataType recognizeExprDataType(@NonNull String partitionKeyExpression);

}
