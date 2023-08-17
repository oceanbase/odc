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

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link InLineCheckConstraint}
 *
 * @author yh263208
 * @date 2023-05-19 18:23
 * @since ODC_release_4.2.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class InLineCheckConstraint extends InLineConstraint {

    private final Expression checkExpr;

    public InLineCheckConstraint(@NonNull ParserRuleContext context,
            String constraintName, ConstraintState state, @NonNull Expression checkExpr) {
        super(context, constraintName, state);
        this.checkExpr = checkExpr;
    }

    public InLineCheckConstraint(String constraintName,
            ConstraintState state, @NonNull Expression checkExpr) {
        super(constraintName, state);
        this.checkExpr = checkExpr;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getConstraintName() != null) {
            builder.append(" CONSTRAINT ").append(getConstraintName());
        }
        builder.append(" CHECK(").append(this.checkExpr).append(")");
        if (getState() != null) {
            builder.append(" ").append(getState());
        }
        return builder.substring(1);
    }

}
