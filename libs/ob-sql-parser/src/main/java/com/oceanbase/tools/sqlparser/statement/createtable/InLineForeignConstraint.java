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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link InLineForeignConstraint}
 *
 * @author yh263208
 * @date 2023-05-19 19:57
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class InLineForeignConstraint extends InLineConstraint {

    private final ForeignReference reference;

    public InLineForeignConstraint(@NonNull ParserRuleContext context,
            String constraintName, ConstraintState state, @NonNull ForeignReference reference) {
        super(context, constraintName, state);
        this.reference = reference;
    }

    public InLineForeignConstraint(String constraintName,
            ConstraintState state, @NonNull ForeignReference reference) {
        super(constraintName, state);
        this.reference = reference;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getConstraintName() != null) {
            builder.append(" CONSTRAINT ").append(getConstraintName());
        }
        builder.append(" ").append(this.reference);
        if (getState() != null) {
            builder.append(" ").append(getState());
        }
        return builder.substring(1);
    }
}
