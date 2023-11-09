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

package com.oceanbase.tools.sqlparser.statement.common.oracle;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link KeepClause}
 *
 * @author yh263208
 * @date 2023-09-28 10:31
 * @since ODC_release_4.2.2
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class KeepClause extends BaseStatement {

    private final String denseRank;
    private final OrderBy orderBy;

    public KeepClause(@NonNull TerminalNode beginNode, @NonNull ParserRuleContext endRule,
            @NonNull String denseRank, @NonNull OrderBy orderBy) {
        super(beginNode, endRule);
        this.orderBy = orderBy;
        this.denseRank = denseRank;
    }

    public KeepClause(@NonNull String denseRank, @NonNull OrderBy orderBy) {
        this.orderBy = orderBy;
        this.denseRank = denseRank;
    }

    @Override
    public String toString() {
        return "DENSE_RANK " + this.denseRank + " " + this.orderBy;
    }

}
