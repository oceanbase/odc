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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link ConstExpression}
 *
 * @author yh263208
 * @date 2022-11-28 17:27
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ConstExpression extends BaseExpression {

    private final String exprConst;

    public ConstExpression(TerminalNode terminalNode) {
        super(terminalNode);
        this.exprConst = getText();
    }

    public ConstExpression(ParserRuleContext ruleNode) {
        super(ruleNode);
        this.exprConst = getText();
    }

    public ConstExpression(@NonNull String exprConst) {
        this.exprConst = exprConst;
    }

    public ConstExpression(ParserRuleContext beginRule, TerminalNode endNode) {
        super(beginRule, endNode);
        this.exprConst = getText();
    }

    public ConstExpression(TerminalNode beginNode, TerminalNode endNode) {
        super(beginNode, endNode);
        this.exprConst = getText();
    }

    public ConstExpression(TerminalNode beginNode, ParserRuleContext endRule) {
        super(beginNode, endRule);
        this.exprConst = getText();

    }

    public ConstExpression(ParserRuleContext beginRule, ParserRuleContext endRule) {
        super(beginRule, endRule);
        this.exprConst = getText();
    }

    @Override
    public String doToString() {
        return this.exprConst;
    }

}
