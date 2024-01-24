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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;

import lombok.NonNull;

/**
 * {@link OBMySQLAutoPartitionKeyDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-23 17:01
 * @since ODC_release_4.2.4
 */
public class OBMySQLAutoPartitionKeyDataTypeFactory extends BaseAutoPartitionKeyDataTypeFactory {

    private static final Map<String, Integer> TIME_FUNCTION_OR_TYPE_NAME_2_PREC = new HashMap<>();

    static {
        TIME_FUNCTION_OR_TYPE_NAME_2_PREC.put("date", TimeDataType.DAY);
        TIME_FUNCTION_OR_TYPE_NAME_2_PREC.put("datetime", TimeDataType.SECOND);
        TIME_FUNCTION_OR_TYPE_NAME_2_PREC.put("timestamp", TimeDataType.SECOND);
        TIME_FUNCTION_OR_TYPE_NAME_2_PREC.put("str_to_date", TimeDataType.DAY);
    }

    public OBMySQLAutoPartitionKeyDataTypeFactory(@NonNull SqlExprCalculator calculator,
            @NonNull DBTable dbTable, @NonNull String partitionKey) {
        super(calculator, dbTable, partitionKey);
    }

    @Override
    protected String unquoteIdentifier(String identifier) {
        return identifier == null ? null : StringUtils.unquoteMySqlIdentifier(identifier);
    }

    @Override
    protected DataType afterConvert(DataType target) {
        DataType dataType = recognizeTimeDataType(target.getDataTypeName());
        return dataType == null ? target : dataType;
    }

    @Override
    protected DataType preRecognizeExprDataType(@NonNull String partitionKeyExpression) {
        Statement statement;
        try {
            statement = new ExpressionParser().parse(new StringReader(partitionKeyExpression));
        } catch (Exception e) {
            statement = null;
        }
        if (!(statement instanceof FunctionCall)) {
            return null;
        }
        FunctionCall f = (FunctionCall) statement;
        return recognizeTimeDataType(f.getFunctionName());
    }

    private DataType recognizeTimeDataType(@NonNull String funcNameOrTypeName) {
        Integer prec = TIME_FUNCTION_OR_TYPE_NAME_2_PREC.get(funcNameOrTypeName.toLowerCase());
        return prec == null ? null : new TimeDataType(prec);
    }

    static private class ExpressionParser extends OBMySQLParser {
        @Override
        protected ParseTree doParse(OBParser parser) {
            return parser.expr();
        }
    }

}
