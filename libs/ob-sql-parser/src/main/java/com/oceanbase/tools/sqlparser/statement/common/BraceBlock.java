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

package com.oceanbase.tools.sqlparser.statement.common;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.BaseExpression;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link BraceBlock}
 *
 * @author yh263208
 * @date 2023-09-21 14:47
 * @since ODC_release_4.2.2
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class BraceBlock extends BaseExpression implements FromReference {

    private final Statement wrappedTarget;
    private final String relation;

    public BraceBlock(@NonNull ParserRuleContext context, String relation, Statement wrappedTarget) {
        super(context);
        this.wrappedTarget = wrappedTarget;
        this.relation = relation;
    }

    public BraceBlock(String relation, Statement wrappedTarget) {
        this.wrappedTarget = wrappedTarget;
        this.relation = relation;
    }

    @Override
    protected String doToString() {
        StringBuilder builder = new StringBuilder("{");
        if (this.relation != null) {
            builder.append(this.relation);
        }
        if (this.wrappedTarget != null) {
            builder.append(" ").append(this.wrappedTarget);
        }
        return builder.append("}").toString();
    }

}
