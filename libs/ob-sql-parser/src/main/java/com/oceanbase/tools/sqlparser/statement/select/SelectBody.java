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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.Window;
import com.oceanbase.tools.sqlparser.statement.expression.BaseExpression;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link SelectBody}
 *
 * @author yh263208
 * @date 2022-11-24 20:42
 * @since ODC-release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SelectBody extends BaseExpression {

    private Expression where;
    private Expression having;
    private String queryOptions;
    private Expression startWith;
    private Expression connectBy;
    private List<WithTable> with = new ArrayList<>();
    private boolean recursive;
    private List<GroupBy> groupBy = new ArrayList<>();
    private boolean withRollUp;
    private boolean withCheckOption;
    private boolean approximate;
    private List<Window> windows = new ArrayList<>();
    private Fetch fetch;
    private Limit limit;
    private ForUpdate forUpdate;
    private OrderBy orderBy;
    private boolean lockInShareMode;

    private RelatedSelectBody relatedSelect;
    private final List<List<Expression>> values;
    private final List<FromReference> froms;
    private final List<Projection> selectItems;

    public SelectBody(@NonNull ParserRuleContext context,
            @NonNull List<Projection> selectItemList, @NonNull List<FromReference> fromList) {
        super(context);
        this.froms = fromList;
        this.selectItems = selectItemList;
        this.values = Collections.emptyList();
    }

    public SelectBody(@NonNull ParserRuleContext context, @NonNull List<List<Expression>> values) {
        super(context);
        this.froms = Collections.emptyList();
        this.selectItems = Collections.emptyList();
        this.values = values;
    }

    public SelectBody(@NonNull ParserRuleContext context, @NonNull SelectBody other) {
        super(context);
        this.where = other.where;
        this.having = other.having;
        this.queryOptions = other.queryOptions;
        this.startWith = other.startWith;
        this.connectBy = other.connectBy;
        this.with = other.with;
        this.recursive = other.recursive;
        this.groupBy = other.groupBy;
        this.lockInShareMode = other.lockInShareMode;
        this.withRollUp = other.withRollUp;
        this.withCheckOption = other.withCheckOption;
        this.windows = other.windows;
        this.fetch = other.fetch;
        this.limit = other.limit;
        this.orderBy = other.orderBy;
        this.relatedSelect = other.relatedSelect;
        this.froms = other.froms;
        this.selectItems = other.selectItems;
        this.forUpdate = other.forUpdate;
        this.approximate = other.approximate;
        this.values = other.values;
    }

    public SelectBody getLastSelectBody() {
        SelectBody target = this;
        while (target.getRelatedSelect() != null) {
            target = target.getRelatedSelect().getSelect();
        }
        return target;
    }

    public SelectBody(@NonNull List<Projection> selectItemList, @NonNull List<FromReference> fromList) {
        this.froms = fromList;
        this.selectItems = selectItemList;
        this.values = Collections.emptyList();
    }

    public SelectBody(@NonNull List<List<Expression>> values) {
        this.froms = Collections.emptyList();
        this.selectItems = Collections.emptyList();
        this.values = values;
    }

    @Override
    public String doToString() {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(this.with)) {
            builder.append("WITH ");
            if (this.recursive) {
                builder.append("RECURSIVE ");
            }
            builder.append(this.with.stream()
                    .map(WithTable::toString).collect(Collectors.joining(","))).append(" ");
        }
        if (this.orderBy != null || this.fetch != null || this.limit != null || this.forUpdate != null) {
            builder.append("(");
        }
        if (CollectionUtils.isNotEmpty(this.values)) {
            builder.append("VALUES ").append(this.values.stream()
                    .map(s -> "ROW (" + s.stream().map(Object::toString)
                            .collect(Collectors.joining(",")) + ")")
                    .collect(Collectors.joining(", ")));
        } else {
            builder.append("SELECT");
            if (this.queryOptions != null) {
                builder.append(" ").append(this.queryOptions);
            }
            builder.append(" ").append(this.selectItems.stream()
                    .map(Projection::toString).collect(Collectors.joining(",")));
            if (CollectionUtils.isNotEmpty(this.froms)) {
                builder.append(" FROM ").append(this.froms.stream()
                        .map(Object::toString).collect(Collectors.joining(",")));
            }
            if (this.where != null) {
                builder.append(" WHERE ").append(this.where.toString());
            }
            if (this.startWith != null) {
                builder.append(" START WITH ").append(this.startWith.toString());
            }
            if (this.connectBy != null) {
                builder.append(" CONNECT BY ").append(this.connectBy.toString());
            }
            if (CollectionUtils.isNotEmpty(this.groupBy)) {
                builder.append(" GROUP BY ").append(this.groupBy.stream()
                        .map(Object::toString).collect(Collectors.joining(",")));
                if (this.withRollUp) {
                    builder.append(" WITH ROLLUP");
                }
            }
            if (this.having != null) {
                builder.append(" HAVING ").append(this.having.toString());
            }
            if (CollectionUtils.isNotEmpty(this.windows)) {
                builder.append(" WINDOW ").append(this.windows.stream()
                        .map(Window::toString).collect(Collectors.joining(",")));
            }
        }
        if (this.orderBy != null) {
            builder.append(" ").append(this.orderBy.toString());
        }
        if (this.approximate) {
            builder.append(" APPROXIMATE");
        }
        if (this.fetch != null) {
            builder.append(" ").append(this.fetch.toString());
        }
        if (this.withCheckOption) {
            builder.append(" WITH CHECK OPTION");
        }
        if (this.limit != null) {
            builder.append(" ").append(this.limit.toString());
        }
        if (this.forUpdate != null) {
            builder.append(" ").append(this.forUpdate.toString());
        }
        if (this.lockInShareMode) {
            builder.append(" LOCK IN SHARE MODE");
        }
        if (this.orderBy != null || this.fetch != null || this.limit != null || this.forUpdate != null) {
            builder.append(")");
        }
        if (this.relatedSelect != null) {
            builder.append(" ").append(this.relatedSelect.toString());
        }
        return builder.toString();
    }

}
