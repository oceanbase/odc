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
package com.oceanbase.tools.sqlparser.statement.alter.table;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link PartitionSplitActions}
 *
 * @author yh263208
 * @date 2023-06-13 17:11
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PartitionSplitActions extends BaseStatement {

    private List<Expression> listExprs;
    private List<Expression> rangeExprs;
    private List<PartitionElement> intos;

    public PartitionSplitActions(@NonNull ParserRuleContext context) {
        super(context);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.listExprs != null) {
            builder.append(" VALUES (")
                    .append(this.listExprs.stream().map(Object::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        } else if (this.rangeExprs != null) {
            builder.append(" AT (")
                    .append(this.rangeExprs.stream().map(Object::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        if (CollectionUtils.isNotEmpty(this.intos)) {
            builder.append(" INTO (")
                    .append(this.intos.stream().map(Object::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        return builder.length() == 0 ? null : builder.substring(1);
    }

}
