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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SearchMode;
import com.oceanbase.tools.sqlparser.statement.select.oracle.SetValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link WithTable}
 *
 * @author yh263208
 * @date 2022-12-07 16:49
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class WithTable extends BaseStatement {

    private final String relation;
    private final SelectBody select;
    private List<String> aliasList;
    private SearchMode searchMode;
    private List<SortKey> searchSortKeyList;
    private SetValue searchValueSet;
    private SetValue cycleValueSet;
    private List<String> cycleAliasList;

    public WithTable(@NonNull ParserRuleContext context,
            @NonNull String relation, @NonNull SelectBody select) {
        super(context);
        this.relation = relation;
        this.select = select;
    }

    public WithTable(@NonNull String relation, @NonNull SelectBody select) {
        this.relation = relation;
        this.select = select;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.relation);
        if (CollectionUtils.isNotEmpty(this.aliasList)) {
            builder.append(" (").append(String.join(",", this.aliasList)).append(")");
        }
        builder.append(" AS (").append(this.select.toString()).append(")");
        if (this.searchMode != null) {
            builder.append(" SEARCH ").append(this.searchMode.name())
                    .append(" BY ").append(this.searchSortKeyList.stream().map(SortKey::toString)
                            .collect(Collectors.joining(",")))
                    .append(" ").append(this.searchValueSet.toString());
        }
        if (CollectionUtils.isNotEmpty(this.cycleAliasList)) {
            builder.append(" CYCLE ").append(String.join(",", this.cycleAliasList)).append(" ")
                    .append(this.cycleValueSet.toString());
        }
        return builder.toString();
    }

}
