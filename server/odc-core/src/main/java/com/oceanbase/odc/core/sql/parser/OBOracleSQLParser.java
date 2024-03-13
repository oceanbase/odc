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
package com.oceanbase.odc.core.sql.parser;

import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * {@link OBOracleSQLParser}
 *
 * @author yh263208
 * @date 2023-07-25 11:48
 * @since ODC_release_4.2.0
 */
class OBOracleSQLParser extends com.oceanbase.tools.sqlparser.OBOracleSQLParser {

    @Override
    public Statement buildStatement(ParseTree root) {
        Statement statement = super.buildStatement(root);
        if (statement != null) {
            return statement;
        }
        statement = new OBOracleDropStatementVisitor().visit(root);
        if (statement != null) {
            return statement;
        }
        return new OBOracleCreateStatementVisitor().visit(root);
    }

}
