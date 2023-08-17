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
package com.oceanbase.tools.sqlparser.adapter.mysql;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Named_windowContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.Window;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;

import lombok.NonNull;

/**
 * {@link MySQLWindowFactory}
 *
 * @author yh263208
 * @date 2022-12-12 15:48
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLWindowFactory extends OBParserBaseVisitor<Window> implements StatementFactory<Window> {

    private final Named_windowContext namedWindowContext;

    public MySQLWindowFactory(@NonNull Named_windowContext namedWindowContext) {
        this.namedWindowContext = namedWindowContext;
    }

    @Override
    public Window generate() {
        return visit(this.namedWindowContext);
    }

    @Override
    public Window visitNamed_window(Named_windowContext ctx) {
        StatementFactory<WindowSpec> factory =
                new MySQLWindowSpecFactory(ctx.new_generalized_window_clause_with_blanket());
        return new Window(ctx, ctx.NAME_OB().getText(), factory.generate());
    }

}
