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

import java.math.BigDecimal;
import java.util.ArrayList;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.For_update_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Opt_for_update_waitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.WaitOption;

import lombok.NonNull;

/**
 * {@link MySQLForUpdateFactory}
 *
 * @author yh263208
 * @date 2022-12-12 16:59
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLForUpdateFactory extends OBParserBaseVisitor<ForUpdate> implements StatementFactory<ForUpdate> {

    private final For_update_clauseContext forUpdateClauseContext;

    public MySQLForUpdateFactory(@NonNull For_update_clauseContext forUpdateClauseContext) {
        this.forUpdateClauseContext = forUpdateClauseContext;
    }

    @Override
    public ForUpdate generate() {
        return visit(this.forUpdateClauseContext);
    }

    @Override
    public ForUpdate visitFor_update_clause(For_update_clauseContext ctx) {
        Opt_for_update_waitContext opt = ctx.opt_for_update_wait();
        if (opt == null || opt.empty() != null) {
            return new ForUpdate(ctx, new ArrayList<>(), null, null);
        }
        if (opt.NO_WAIT() != null || opt.NOWAIT() != null) {
            return new ForUpdate(ctx, new ArrayList<>(), WaitOption.NOWAIT, null);
        }
        BigDecimal waitNum;
        if (opt.DECIMAL_VAL() != null) {
            waitNum = new BigDecimal(opt.DECIMAL_VAL().getText());
        } else {
            waitNum = new BigDecimal(opt.INTNUM().getText());
        }
        return new ForUpdate(ctx, new ArrayList<>(), WaitOption.WAIT, waitNum);
    }

}
