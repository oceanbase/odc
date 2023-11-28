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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Log_error_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Reject_limitContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.oracle.LogErrors;

import lombok.NonNull;

/**
 * {@link OracleLogErrorsFactory}
 *
 * @author yh263208
 * @date 2023-11-08 20:15
 * @since ODC_release_4.2.3
 */
public class OracleLogErrorsFactory extends OBParserBaseVisitor<LogErrors> implements StatementFactory<LogErrors> {

    private final ParserRuleContext context;

    public OracleLogErrorsFactory(@NonNull Log_error_clauseContext clauseContext) {
        this.context = clauseContext;
    }

    @Override
    public LogErrors generate() {
        return visit(this.context);
    }

    @Override
    public LogErrors visitLog_error_clause(Log_error_clauseContext ctx) {
        LogErrors logErrors = new LogErrors(ctx);
        if (ctx.into_err_log_caluse().relation_factor() != null) {
            logErrors.setInto(OracleFromReferenceFactory.getRelationFactor(
                    ctx.into_err_log_caluse().relation_factor()));
        }
        if (ctx.opt_simple_expression().simple_expr() != null) {
            logErrors.setExpression(new OracleExpressionFactory(
                    ctx.opt_simple_expression().simple_expr()).generate());
        }
        if (ctx.reject_limit().REJECT() != null) {
            Reject_limitContext rCtx = ctx.reject_limit();
            if (rCtx.INTNUM() != null) {
                logErrors.setUnlimitedReject(false);
                logErrors.setRejectLimit(Integer.valueOf(rCtx.INTNUM().getText()));
            } else {
                logErrors.setRejectLimit(null);
                logErrors.setUnlimitedReject(true);
            }
        }
        return logErrors;
    }

}
