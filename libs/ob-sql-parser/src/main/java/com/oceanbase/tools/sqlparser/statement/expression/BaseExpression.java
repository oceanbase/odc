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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Exclude;
import lombok.NonNull;

/**
 * {@link BaseExpression}
 *
 * @author yh263208
 * @date 2023-09-22 21:01
 * @since ODC_release_4.2.2
 */
@EqualsAndHashCode(callSuper = false)
public abstract class BaseExpression extends BaseStatement implements Expression {

    private Expression nextExpr;
    private ReferenceOperator nextOperator;
    @Exclude
    private Expression parentExpr;
    @Exclude
    private ReferenceOperator parentOperator;

    protected BaseExpression() {
        super();
    }

    protected BaseExpression(TerminalNode terminalNode) {
        super(terminalNode);
    }

    protected BaseExpression(ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    protected BaseExpression(ParserRuleContext beginRule, TerminalNode endNode) {
        super(beginRule, endNode);
    }

    protected BaseExpression(TerminalNode beginNode, TerminalNode endNode) {
        super(beginNode, endNode);
    }

    protected BaseExpression(TerminalNode beginNode, ParserRuleContext endRule) {
        super(beginNode, endRule);
    }

    protected BaseExpression(ParserRuleContext beginRule, ParserRuleContext endRule) {
        super(beginRule, endRule);
    }

    @Override
    public Expression reference(@NonNull Expression nextExpr, @NonNull ReferenceOperator operator) {
        if (this.nextExpr == nextExpr && this.nextOperator == operator) {
            return nextExpr;
        }
        this.nextExpr = nextExpr;
        this.nextOperator = operator;
        nextExpr.parentReference(this, operator);
        return nextExpr;
    }

    @Override
    public Expression parentReference(@NonNull Expression parentExpr, @NonNull ReferenceOperator operator) {
        if (this.parentExpr == parentExpr && this.parentOperator == operator) {
            return parentExpr;
        }
        this.parentExpr = parentExpr;
        this.parentOperator = operator;
        parentExpr.reference(this, operator);
        return parentExpr;
    }

    @Override
    public Expression getReference() {
        return this.nextExpr;
    }

    @Override
    public Expression getParentReference() {
        return this.parentExpr;
    }

    @Override
    public ReferenceOperator getReferenceOperator() {
        return this.nextOperator;
    }

    @Override
    public ReferenceOperator getParentReferenceOperator() {
        return this.parentOperator;
    }

    abstract protected String doToString();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String toString = doToString();
        if (StringUtils.isEmpty(toString)) {
            toString = "";
        }
        if (this.parentExpr != null && this.parentOperator != null) {
            builder.append(this.parentOperator.wrap(toString));
        } else {
            builder.append(toString);
        }
        if (this.nextExpr == null) {
            return builder.toString();
        }
        return builder.append(this.nextExpr.toString()).toString();
    }

}
