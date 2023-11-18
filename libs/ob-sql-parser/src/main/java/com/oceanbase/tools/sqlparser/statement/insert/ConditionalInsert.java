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

package com.oceanbase.tools.sqlparser.statement.insert;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ConditionalInsert}
 *
 * @author yh263208
 * @date 2023-11-08 17:55
 * @since ODC_release_4.2.3
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConditionalInsert extends BaseStatement {

    private List<InsertTable> elseClause = Collections.emptyList();
    private final List<InsertCondition> conditions;

    public ConditionalInsert(@NonNull ParserRuleContext context,
            @NonNull List<InsertCondition> conditions) {
        super(context);
        this.conditions = conditions;
    }

    public ConditionalInsert(@NonNull ParserRuleContext begin,
            @NonNull ParserRuleContext end,
            @NonNull List<InsertCondition> conditions) {
        super(begin, end);
        this.conditions = conditions;
    }

    public ConditionalInsert(@NonNull List<InsertCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(this.conditions.stream()
                .map(InsertCondition::toString)
                .collect(Collectors.joining("\n")));
        if (CollectionUtils.isNotEmpty(this.elseClause)) {
            builder.append("\nELSE\n\t").append(this.elseClause.stream()
                    .map(InsertTable::toString).collect(Collectors.joining("\n\t")));
        }
        return builder.toString();
    }

}
