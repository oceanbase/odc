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
package com.oceanbase.tools.sqlparser.statement.createtable;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.sequence.SequenceOptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link GenerateOption}
 *
 * @author yh263208
 * @date 2023-05-18 21:14
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class GenerateOption extends BaseStatement {
    /**
     * candidate:
     * 
     * <pre>
     *     1. always
     *     2. by default on null
     * </pre>
     */
    private String generateOption;
    private Type type;
    private final Expression asExpression;
    private final boolean asIdentity;
    private final SequenceOptions sequenceOptions;

    public GenerateOption(@NonNull ParserRuleContext context,
            @NonNull Expression asExpression) {
        super(context);
        this.asExpression = asExpression;
        this.asIdentity = false;
        this.sequenceOptions = null;
    }

    public GenerateOption(@NonNull ParserRuleContext context,
            SequenceOptions sequenceOptions) {
        super(context);
        this.asExpression = null;
        this.asIdentity = true;
        this.sequenceOptions = sequenceOptions;
    }

    public GenerateOption(@NonNull Expression asExpression) {
        this.asExpression = asExpression;
        this.asIdentity = false;
        this.sequenceOptions = null;
    }

    public GenerateOption(SequenceOptions sequenceOptions) {
        this.asExpression = null;
        this.asIdentity = true;
        this.sequenceOptions = sequenceOptions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.generateOption != null) {
            builder.append(" GENERATED ").append(this.generateOption.toUpperCase());
        }
        builder.append(" AS");
        if (this.asExpression != null) {
            builder.append(" (").append(this.asExpression.toString()).append(")");
        } else if (this.asIdentity) {
            builder.append(" IDENTITY");
            if (this.sequenceOptions != null) {
                builder.append(" ").append(this.sequenceOptions);
            }
        }
        if (this.type != null) {
            builder.append(" ").append(this.type.name());
        }
        return builder.substring(1);
    }

    public enum Type {
        // virtual column
        VIRTUAL,
        // stored column
        STORED
    }

}
