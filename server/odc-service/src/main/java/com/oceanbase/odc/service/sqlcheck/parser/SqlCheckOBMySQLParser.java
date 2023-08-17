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
package com.oceanbase.odc.service.sqlcheck.parser;

import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * {@link SqlCheckOBMySQLParser}
 *
 * @author yh263208
 * @date 2023-07-25 14:39
 * @since ODC_release_4.2.0
 */
public class SqlCheckOBMySQLParser extends OBMySQLParser {

    @Override
    public Statement buildStatement(ParseTree root) {
        Statement statement = super.buildStatement(root);
        if (statement != null) {
            return statement;
        }
        return new OBMySQLDropStatementVisitor().visit(root);
    }

}
