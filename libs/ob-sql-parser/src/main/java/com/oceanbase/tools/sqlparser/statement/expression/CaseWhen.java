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
package com.oceanbase.tools.sqlparser.statement.expression;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author gaoda.xy
 * @date 2023/6/26 14:33
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class CaseWhen extends BaseStatement implements Expression {

    private Expression caseValue;
    private Expression caseDefault;
    private final List<WhenClause> whenClauses;

    public CaseWhen(@NonNull ParserRuleContext context, @NonNull List<WhenClause> whenClauses) {
        super(context);
        this.whenClauses = whenClauses;
    }

    public CaseWhen(@NonNull List<WhenClause> whenClauses) {
        this.whenClauses = whenClauses;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CASE");
        if (this.caseValue != null) {
            builder.append(" ").append(this.caseValue);
        }
        if (CollectionUtils.isNotEmpty(this.whenClauses)) {
            builder.append("\n\t").append(this.whenClauses.stream()
                    .map(WhenClause::toString).collect(Collectors.joining("\n\t")));
        }
        if (this.caseDefault != null) {
            builder.append("\n\tELSE ").append(this.caseDefault);
        }
        builder.append("\nEND");
        return builder.toString();
    }

}
