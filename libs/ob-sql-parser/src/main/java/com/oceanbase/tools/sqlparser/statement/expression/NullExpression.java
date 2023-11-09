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

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * {@link NullExpression}
 *
 * @author yh263208
 * @date 2022-11-29 12:19
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NullExpression extends BaseExpression {

    public NullExpression(TerminalNode terminalNode) {
        super(terminalNode);
    }

    public NullExpression(ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    @Override
    public String doToString() {
        return "NULL";
    }

}
