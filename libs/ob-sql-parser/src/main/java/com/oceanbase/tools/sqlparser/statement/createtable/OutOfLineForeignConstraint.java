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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link OutOfLineForeignConstraint}
 *
 * @author yh263208
 * @date 2023-05-24 17:37
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class OutOfLineForeignConstraint extends OutOfLineConstraint {

    private final ForeignReference reference;

    public OutOfLineForeignConstraint(@NonNull ParserRuleContext context,
            ConstraintState state, @NonNull List<SortColumn> columns, @NonNull ForeignReference reference) {
        super(context, state, columns);
        this.reference = reference;
    }

    public OutOfLineForeignConstraint(ConstraintState state,
            @NonNull List<SortColumn> columns, @NonNull ForeignReference reference) {
        super(state, columns);
        this.reference = reference;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getConstraintName() != null) {
            builder.append(" CONSTRAINT ").append(getConstraintName());
        }
        builder.append(" FOREIGN KEY(")
                .append(getColumns().stream().map(SortColumn::toString)
                        .collect(Collectors.joining(",")))
                .append(") ").append(this.reference);
        if (getState() != null) {
            builder.append(" ").append(getState());
        }
        return builder.substring(1);
    }

}
