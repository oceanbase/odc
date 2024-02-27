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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;

import lombok.NonNull;

/**
 * {@link OBMySQLPartitionKeyDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-23 17:01
 * @since ODC_release_4.2.4
 */
public class OBMySQLPartitionKeyDataTypeFactory extends BasePartitionKeyDataTypeFactory {

    private static final Map<String, Integer> FUNCTION_NAME_2_PREC = new HashMap<>();

    static {
        FUNCTION_NAME_2_PREC.put("from_days", TimeDataType.DAY);
        FUNCTION_NAME_2_PREC.put("str_to_date", TimeDataType.DAY);
    }

    public OBMySQLPartitionKeyDataTypeFactory(@NonNull SqlExprCalculator calculator,
            @NonNull DBTable dbTable, @NonNull String partitionKey) {
        super(calculator, dbTable, partitionKey);
    }

    @Override
    protected String unquoteIdentifier(String identifier) {
        return new OBMySQLAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    @Override
    protected DataType recognizeColumnDataType(@NonNull DBTableColumn column) {
        return new OBMySQLCommonDataTypeFactory(column.getTypeName()).generate();
    }

    @Override
    protected DataType recognizeExprDataType(@NonNull DBTable dbTable, @NonNull String partitionKey) {
        Statement statement;
        try {
            statement = new ExpressionParser().parse(new StringReader(partitionKey));
        } catch (Exception e) {
            statement = null;
        }
        if (!(statement instanceof FunctionCall)) {
            return null;
        }
        FunctionCall f = (FunctionCall) statement;
        Integer prec = FUNCTION_NAME_2_PREC.get(f.getFunctionName().toLowerCase());
        return prec == null ? null : new TimeDataType("date", prec);
    }

    static private class ExpressionParser extends OBMySQLParser {
        @Override
        protected ParseTree doParse(OBParser parser) {
            return parser.expr();
        }
    }

}
