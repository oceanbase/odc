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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link InLineConstraint}
 *
 * @author yh263208
 * @date 2023-05-19 17:56
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class InLineConstraint extends BaseStatement {

    private boolean uniqueKey;
    private boolean primaryKey;
    private Boolean nullable;
    private final String constraintName;
    private final ConstraintState state;

    public InLineConstraint(@NonNull ParserRuleContext context,
            String constraintName, ConstraintState state) {
        super(context);
        this.state = state;
        this.constraintName = constraintName;
    }

    public InLineConstraint(String constraintName,
            ConstraintState state) {
        this.state = state;
        this.constraintName = constraintName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.constraintName != null) {
            builder.append(" CONSTRAINT ").append(this.constraintName);
        }
        if (this.isPrimaryKey()) {
            builder.append(" PRIMARY KEY");
            return builder.substring(1);
        } else if (this.isUniqueKey()) {
            builder.append(" UNIQUE");
            return builder.substring(1);
        }
        if (this.nullable != null) {
            if (this.nullable) {
                builder.append(" NULL");
            } else {
                builder.append(" NOT NULL");
            }
        }
        if (this.state != null) {
            builder.append(" ").append(this.state.toString());
        }
        return builder.length() >= 1 ? builder.substring(1) : "";
    }

}
