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

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

/**
 * {@link FunctionParam}
 *
 * @author yh263208 '
 * @date 2022-11-25 18:03
 * @since ODC_release_4.1.0
 * @see Expression
 */
public class FunctionParam extends BaseStatement {

    protected FunctionParam() {
        super(null, null);
    }

    protected FunctionParam(ParserRuleContext ruleNode) {
        super(ruleNode, null);
    }

}
