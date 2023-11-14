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

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link JsonKeyValue}
 *
 * @author yh263208
 * @date 2023-09-27 10:07
 * @since ODC_rleease_4.2.2
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class JsonKeyValue extends BaseExpression {

    private final Expression key;
    private final Expression value;

    public JsonKeyValue(@NonNull ParserRuleContext context,
            @NonNull Expression key, @NonNull Expression value) {
        super(context);
        this.key = key;
        this.value = value;
    }

    public JsonKeyValue(@NonNull ParserRuleContext beginRule,
            @NonNull ParserRuleContext endRule,
            @NonNull Expression key, @NonNull Expression value) {
        super(beginRule, endRule);
        this.key = key;
        this.value = value;
    }

    public JsonKeyValue(@NonNull TerminalNode beginNode,
            @NonNull ParserRuleContext endRule,
            @NonNull Expression key, @NonNull Expression value) {
        super(beginNode, endRule);
        this.key = key;
        this.value = value;
    }

    public JsonKeyValue(@NonNull Expression key, @NonNull Expression value) {
        this.key = key;
        this.value = value;
    }

    @Override
    protected String doToString() {
        return "KEY " + this.key + " VALUE " + this.value;
    }

}
