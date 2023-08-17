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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Sequence_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Simple_numContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.sequence.SequenceOptions;

import lombok.NonNull;

/**
 * {@link OracleSequenceOptionsFactory}
 *
 * @author yh263208
 * @date 2023-06-01 14:54
 * @since ODC_release_4.2.0
 */
public class OracleSequenceOptionsFactory extends OBParserBaseVisitor<SequenceOptions>
        implements StatementFactory<SequenceOptions> {

    private final Sequence_option_listContext sequenceOptionListContext;

    public OracleSequenceOptionsFactory(@NonNull Sequence_option_listContext sequenceOptionListContext) {
        this.sequenceOptionListContext = sequenceOptionListContext;
    }

    @Override
    public SequenceOptions generate() {
        return visit(this.sequenceOptionListContext);
    }

    @Override
    public SequenceOptions visitSequence_option_list(Sequence_option_listContext ctx) {
        SequenceOptions sequenceOptions = new SequenceOptions(ctx);
        ctx.sequence_option().forEach(context -> {
            if (context.NOMAXVALUE() != null) {
                sequenceOptions.setNoMaxValue(true);
            } else if (context.NOMINVALUE() != null) {
                sequenceOptions.setNoMinValue(true);
            } else if (context.CYCLE() != null) {
                sequenceOptions.setCycle(true);
            } else if (context.NOCYCLE() != null) {
                sequenceOptions.setNoCycle(true);
            } else if (context.CACHE() != null) {
                sequenceOptions.setCache(getNum(context.simple_num()));
            } else if (context.NOCACHE() != null) {
                sequenceOptions.setNoCache(true);
            } else if (context.ORDER() != null) {
                sequenceOptions.setOrder(true);
            } else if (context.NOORDER() != null) {
                sequenceOptions.setNoOrder(true);
            } else if (context.INCREMENT() != null && context.BY() != null) {
                sequenceOptions.setIncrementBy(getNum(context.simple_num()));
            } else if (context.MAXVALUE() != null) {
                sequenceOptions.setMaxValue(getNum(context.simple_num()));
            } else if (context.MINVALUE() != null) {
                sequenceOptions.setMinValue(getNum(context.simple_num()));
            } else if (context.START() != null && context.WITH() != null) {
                sequenceOptions.setStartWith(getNum(context.simple_num()));
            }
        });
        return sequenceOptions;
    }

    private BigDecimal getNum(Simple_numContext context) {
        BigDecimal decimal;
        if (context.INTNUM() != null) {
            decimal = new BigDecimal(context.INTNUM().getText());
        } else {
            decimal = new BigDecimal(context.DECIMAL_VAL().getText());
        }
        return context.Minus() != null ? decimal.negate() : decimal;
    }

}
