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
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link JsonOption}
 *
 * @author yh263208
 * @date 2023-09-26 15:09
 * @since ODC_release_4.2.2
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JsonOption extends BaseExpression {

    private boolean truncate;
    private boolean pretty;
    private boolean ascii;
    private boolean asis;
    private boolean multiValue;
    private JsonOnOption onOption;
    private StrictMode strictMode;
    private ScalarsMode scalarsMode;
    private UniqueMode uniqueMode;
    private WrapperMode wrapperMode;

    public JsonOption(@NonNull ParserRuleContext context) {
        super(context);
    }

    public JsonOption(@NonNull TerminalNode terminalNode) {
        super(terminalNode);
    }

    public JsonOption(@NonNull TerminalNode beginNode, @NonNull ParserRuleContext endRule) {
        super(beginNode, endRule);
    }

    public JsonOption(@NonNull ParserRuleContext beginRule, @NonNull ParserRuleContext endRule) {
        super(beginRule, endRule);
    }

    @Override
    protected String doToString() {
        StringBuilder builder = new StringBuilder();
        if (this.strictMode != null) {
            builder.append(" ").append(this.strictMode.name());
        }
        if (this.truncate) {
            builder.append(" TRUNCATE");
        }
        if (this.scalarsMode != null) {
            builder.append(" ").append(this.scalarsMode.name().replace("_", " "));
        }
        if (this.pretty) {
            builder.append(" PRETTY");
        }
        if (this.ascii) {
            builder.append(" ASCII");
        }
        if (this.uniqueMode != null) {
            builder.append(" ").append(this.uniqueMode.name().replace("_", " "));
        }
        if (this.wrapperMode != null) {
            builder.append(" ").append(this.wrapperMode.name().replace("_", " "));
        }
        if (this.asis) {
            builder.append(" ASIS");
        }
        if (this.onOption != null) {
            builder.append(" ").append(this.onOption);
        }
        if (this.multiValue) {
            builder.append(" MULTIVALUE");
        }
        return builder.toString();
    }

    public enum StrictMode {
        // lax mode
        LAX,
        // strict mode
        STRICT
    }

    public enum ScalarsMode {
        // allow scalars
        ALLOW_SCALARS,
        // disallow scalars
        DISALLOW_SCALARS
    }

    public enum UniqueMode {
        // with unique keys
        WITH_UNIQUE_KEYS,
        // without unique keys
        WITHOUT_UNIQUE_KEYS
    }

    public enum WrapperMode {
        // without wrapper
        WITHOUT_WRAPPER,
        // without array wrapper
        WITHOUT_ARRAY_WRAPPER,
        // with wrapper
        WITH_WRAPPER,
        // with array wrapper
        WITH_ARRAY_WRAPPER,
        // with unconditional wrapper
        WITH_UNCONDITIONAL_WRAPPER,
        // with conditional wrapper
        WITH_CONDITIONAL_WRAPPER,
        // with unconditional array wrapper
        WITH_UNCONDITIONAL_ARRAY_WRAPPER,
        // with conditional array wrapper
        WITH_CONDITIONAL_ARRAY_WRAPPER
    }

}
