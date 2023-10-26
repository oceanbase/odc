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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * {@link FunctionParam}
 *
 * @author yh263208 '
 * @date 2022-11-25 18:03
 * @since ODC_release_4.1.0
 * @see Expression
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class FunctionParam extends BaseStatement {

    private final List<Statement> options = new ArrayList<>();

    protected FunctionParam() {
        super();
    }

    protected FunctionParam(ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    public void addOption(Statement paramsOpt) {
        if (paramsOpt == null) {
            return;
        }
        this.options.add(paramsOpt);
    }

}
