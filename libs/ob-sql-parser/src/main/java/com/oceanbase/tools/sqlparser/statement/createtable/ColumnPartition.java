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

import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * {@link ColumnPartition}
 *
 * @author yh263208
 * @date 2023-05-30 16:55
 */
@EqualsAndHashCode(callSuper = true)
public class ColumnPartition extends BasePartition {

    public ColumnPartition(@NonNull ParserRuleContext context,
            @NonNull List<ColumnReference> columns) {
        super(context, columns, null, null, null);
    }

    public ColumnPartition(@NonNull List<ColumnReference> columns) {
        super(columns, null, null, null);
    }

    @Override
    public String toString() {
        return "PARTITION BY COLUMN(" + getPartitionTargets().stream()
                .map(Expression::toString).collect(Collectors.joining(",")) + ")";
    }

}
