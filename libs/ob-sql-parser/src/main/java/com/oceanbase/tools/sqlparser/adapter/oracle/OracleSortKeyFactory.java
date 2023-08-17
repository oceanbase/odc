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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_asc_descContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_ascending_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_null_posContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Sort_keyContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SortNullPosition;

import lombok.NonNull;

/**
 * {@link OracleSortKeyFactory}
 *
 * @author yh263208
 * @date 2022-12-07 17:33
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleSortKeyFactory extends OBParserBaseVisitor<SortKey> implements StatementFactory<SortKey> {

    private final Sort_keyContext sortKeyContext;

    public OracleSortKeyFactory(@NonNull Sort_keyContext sortKeyContext) {
        this.sortKeyContext = sortKeyContext;
    }

    @Override
    public SortKey generate() {
        return visit(this.sortKeyContext);
    }

    @Override
    public SortKey visitSort_key(Sort_keyContext ctx) {
        OracleExpressionFactory factory = new OracleExpressionFactory(ctx.bit_expr());
        Opt_asc_descContext optAscDesc = ctx.opt_asc_desc();
        return new SortKey(ctx, factory.generate(), getSortDirection(optAscDesc), getSortNullPosition(optAscDesc));
    }

    public static SortDirection getSortDirection(Opt_asc_descContext optAscDesc) {
        SortDirection direction = null;
        Opt_ascending_typeContext optAscendingType = optAscDesc.opt_ascending_type();
        if (optAscendingType != null && (optAscendingType.ASC() != null || optAscendingType.DESC() != null)) {
            direction = optAscendingType.ASC() == null ? SortDirection.DESC : SortDirection.ASC;
        }
        return direction;
    }

    public static SortNullPosition getSortNullPosition(Opt_asc_descContext optAscDesc) {
        Opt_null_posContext optNullPos = optAscDesc.opt_null_pos();
        SortNullPosition nullPosition = null;
        if (optNullPos != null && (optNullPos.LAST() != null || optNullPos.FIRST() != null)) {
            nullPosition = optNullPos.LAST() == null ? SortNullPosition.FIRST : SortNullPosition.LAST;
        }
        return nullPosition;
    }

}
