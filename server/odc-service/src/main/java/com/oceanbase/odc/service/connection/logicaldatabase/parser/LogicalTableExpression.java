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
package com.oceanbase.odc.service.connection.logicaldatabase.parser;

import static com.oceanbase.odc.core.shared.constant.ErrorCodes.LogicalTableExpressionNotEvenlyDivided;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.Verify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 11:23
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogicalTableExpression extends BaseLogicalTableExpression {
    private SchemaExpression schemaExpression;
    private TableExpression tableExpression;

    LogicalTableExpression(ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    @Override
    public List<String> evaluate() throws BadExpressionException {
        List<String> schemaNames = schemaExpression.evaluate();
        List<String> tableNames = tableExpression.evaluate();
        Verify.notEmpty(schemaNames, "schemaNames");
        Verify.notEmpty(tableNames, "tableNames");

        List<String> names = new ArrayList<>();
        if (tableExpression.isRepeat()) {
            for (String schemaName : schemaNames) {
                for (String tableName : tableNames) {
                    names.add(schemaName + "." + tableName);
                }
            }
            return names;
        }
        if (CollectionUtils.isEmpty(tableExpression.getSliceRanges())) {
            Verify.singleton(tableNames, "tableNames");
            for (String schemaName : schemaNames) {
                names.add(schemaName + "." + tableNames.get(0));
            }
            return names;
        }
        if (tableNames.size() % schemaNames.size() != 0) {
            throw new BadExpressionException(
                    LogicalTableExpressionNotEvenlyDivided, new Object[] {tableNames.size(), schemaNames.size()},
                    LogicalTableExpressionNotEvenlyDivided
                            .getEnglishMessage(new Object[] {tableNames.size(), schemaNames.size()}));
        }
        int groupCount = tableNames.size() / schemaNames.size();
        for (int i = 0; i < schemaNames.size(); i++) {
            for (int j = groupCount * i; j < groupCount * (i + 1); j++) {
                names.add(schemaNames.get(i) + "." + tableNames.get(j));
            }
        }
        return names;
    }
}
