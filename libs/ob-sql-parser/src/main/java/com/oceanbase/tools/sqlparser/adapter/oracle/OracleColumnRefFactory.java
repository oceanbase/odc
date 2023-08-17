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
package com.oceanbase.tools.sqlparser.adapter.oracle;

import java.util.List;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_definition_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link OracleColumnRefFactory}
 *
 * @author yh263208
 * @date 2022-12-06 16:06
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleColumnRefFactory extends OBParserBaseVisitor<ColumnReference>
        implements StatementFactory<ColumnReference> {

    private final Column_definition_refContext columnDefinitionRefContext;

    public OracleColumnRefFactory(@NonNull Column_definition_refContext columnDefinitionRefContext) {
        this.columnDefinitionRefContext = columnDefinitionRefContext;
    }

    @Override
    public ColumnReference generate() {
        return visit(this.columnDefinitionRefContext);
    }

    @Override
    public ColumnReference visitColumn_definition_ref(Column_definition_refContext ctx) {
        List<Relation_nameContext> relationNames = ctx.relation_name();
        int size = relationNames.size();
        String relation = null;
        String schema = null;
        if (--size >= 0) {
            relation = relationNames.get(size).getText();
        }
        if (--size >= 0) {
            schema = relationNames.get(size).getText();
        }
        return new ColumnReference(ctx, schema, relation, ctx.column_name().getText());
    }

}
