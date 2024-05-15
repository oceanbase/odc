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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.For_updateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.WaitOption;

import lombok.NonNull;

/**
 * {@link OracleForUpdateFactory}
 *
 * @author yh263208
 * @date 2022-12-07 10:58
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleForUpdateFactory extends OBParserBaseVisitor<ForUpdate> implements StatementFactory<ForUpdate> {

    private final For_updateContext forUpdateContext;

    public OracleForUpdateFactory(@NonNull For_updateContext forUpdateContext) {
        this.forUpdateContext = forUpdateContext;
    }

    @Override
    public ForUpdate generate() {
        return visit(this.forUpdateContext);
    }

    @Override
    public ForUpdate visitFor_update(For_updateContext ctx) {
        List<ColumnReference> columnDefRefs = new ArrayList<>();
        if (ctx.column_list() != null) {
            columnDefRefs = ctx.column_list().column_definition_ref().stream().map(c -> {
                StatementFactory<ColumnReference> factory = new OracleColumnRefFactory(c);
                return factory.generate();
            }).collect(Collectors.toList());
        }
        WaitOption waitOption = null;
        BigDecimal waitNum = null;
        if (ctx.DECIMAL_VAL() != null) {
            waitOption = WaitOption.WAIT;
            waitNum = new BigDecimal(ctx.DECIMAL_VAL().getText());
        } else if (ctx.INTNUM() != null) {
            waitOption = WaitOption.WAIT;
            waitNum = new BigDecimal(ctx.INTNUM().getText());
        } else if (ctx.NOWAIT() != null) {
            waitOption = WaitOption.NOWAIT;
        } else if (ctx.R_SKIP() != null) {
            waitOption = WaitOption.SKIP_LOCKED;
        }
        return new ForUpdate(ctx, columnDefRefs, waitOption, waitNum);
    }

}
