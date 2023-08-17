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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Rename_table_actionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTableAction;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.NonNull;

/**
 * {@link OracleRenameTableActionFactory}
 *
 * @author yh263208
 * @date 2023-06-15 16:29
 * @since ODC_release_4.2.0
 */
public class OracleRenameTableActionFactory extends OBParserBaseVisitor<RenameTableAction>
        implements StatementFactory<RenameTableAction> {

    private final Rename_table_actionContext renameTableActionContext;

    public OracleRenameTableActionFactory(@NonNull Rename_table_actionContext renameTableActionContext) {
        this.renameTableActionContext = renameTableActionContext;
    }

    @Override
    public RenameTableAction generate() {
        return visit(this.renameTableActionContext);
    }

    @Override
    public RenameTableAction visitRename_table_action(Rename_table_actionContext ctx) {
        Relation_factorContext from = ctx.relation_factor(0);
        Relation_factorContext to = ctx.relation_factor(1);

        RelationFactor fromFactor = new RelationFactor(from,
                OracleFromReferenceFactory.getRelation(from));
        fromFactor.setSchema(OracleFromReferenceFactory.getSchemaName(from));
        fromFactor.setUserVariable(OracleFromReferenceFactory.getUserVariable(from));

        RelationFactor toFactor = new RelationFactor(to, OracleFromReferenceFactory.getRelation(to));
        toFactor.setSchema(OracleFromReferenceFactory.getSchemaName(to));
        toFactor.setUserVariable(OracleFromReferenceFactory.getUserVariable(to));
        return new RenameTableAction(ctx, fromFactor, toFactor);
    }

}
