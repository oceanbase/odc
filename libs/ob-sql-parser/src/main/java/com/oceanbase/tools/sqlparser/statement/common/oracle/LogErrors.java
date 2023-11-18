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

package com.oceanbase.tools.sqlparser.statement.common.oracle;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link LogErrors}
 *
 * @author yh263208
 * @date 2023-11-08 19:47
 * @since ODC_release_4.2.3
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LogErrors extends BaseStatement {

    private RelationFactor into;
    private Expression expression;
    private Integer rejectLimit;
    private Boolean unlimitedReject;

    public LogErrors(@NonNull ParserRuleContext context) {
        super(context);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("LOG ERRORS");
        if (this.into != null) {
            builder.append(" INTO ").append(this.into);
        }
        if (this.expression != null) {
            builder.append(" (").append(this.expression).append(")");
        }
        if (Boolean.TRUE.equals(this.unlimitedReject)) {
            builder.append(" REJECT LIMIT UNLIMITED");
        } else if (Boolean.FALSE.equals(this.unlimitedReject) && this.rejectLimit != null) {
            builder.append(" REJECT LIMIT ").append(this.rejectLimit);
        }
        return builder.toString();
    }

}
