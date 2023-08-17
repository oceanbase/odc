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
package com.oceanbase.tools.sqlparser.statement.select;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link ForUpdate}
 *
 * @author yh263208
 * @date 2022-12-07 10:51
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class ForUpdate extends BaseStatement {

    private final List<ColumnReference> columns;
    private final BigDecimal waitNum;
    private final WaitOption waitOption;

    public ForUpdate(@NonNull ParserRuleContext context,
            @NonNull List<ColumnReference> columns, WaitOption waitOption, BigDecimal waitNum) {
        super(context);
        this.columns = columns;
        this.waitOption = waitOption;
        this.waitNum = waitNum;
    }

    public ForUpdate(@NonNull List<ColumnReference> columns, WaitOption waitOption, BigDecimal waitNum) {
        this.columns = columns;
        this.waitOption = waitOption;
        this.waitNum = waitNum;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("FOR UPDATE");
        if (!this.columns.isEmpty()) {
            builder.append(" OF ").append(this.columns.stream()
                    .map(ColumnReference::toString).collect(Collectors.joining(",")));
        }
        if (this.waitOption != null) {
            builder.append(" ").append(this.waitOption.name().replace("_", " "));
            if (this.waitOption == WaitOption.WAIT) {
                builder.append(" ").append(this.waitNum.toString());
            }
        }
        return builder.toString();
    }

}
