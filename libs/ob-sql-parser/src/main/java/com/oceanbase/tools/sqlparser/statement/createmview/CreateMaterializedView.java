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
package com.oceanbase.tools.sqlparser.statement.createmview;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.select.Select;

import lombok.*;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/17 20:28
 * @since: 4.3.4
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CreateMaterializedView extends BaseStatement {

    private final RelationFactor viewName;
    private final Select asSelect;
    private List<String> columns;
    private OutOfLineConstraint primaryKey;
    private TableOptions tableOptions;
    private Partition partition;
    /**
     * candidates:
     * 
     * <pre>
     *     1. WITH CHECK OPTION
     *     2. WITH CASCADED CHECK OPTION
     *     3. WITH LOCAL CHECK OPTION
     *     4. WITH READ ONLY
     * </pre>
     */
    private String withOption;
    private List<ColumnGroupElement> columnGroupElements;
    private MaterializedViewOptions viewOptions;

    public CreateMaterializedView(@NonNull ParserRuleContext context,
            @NonNull RelationFactor viewName, @NonNull Select asSelect) {
        super(context);
        this.viewName = viewName;
        this.asSelect = asSelect;
    }

    public CreateMaterializedView(@NonNull RelationFactor viewName, @NonNull Select asSelect) {
        this.viewName = viewName;
        this.asSelect = asSelect;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("CREATE MATERIALIZED VIEW")
                .append(" ").append(this.viewName);
        if (CollectionUtils.isNotEmpty(this.columns) || this.primaryKey != null) {
            builder.append(" (");
            if (CollectionUtils.isNotEmpty(this.columns)) {
                builder.append(String.join(",\n\t", this.columns));
            }
            if (this.primaryKey != null) {
                if (CollectionUtils.isNotEmpty(this.columns)) {
                    builder.append(", ");
                }
                builder.append(this.primaryKey);
            }
            builder.append(")");
        }
        if (this.tableOptions != null) {
            builder.append(" ").append(this.tableOptions);
        }
        if (this.partition != null) {
            builder.append(" ").append(this.partition);
        }
        if (CollectionUtils.isNotEmpty(this.columnGroupElements)) {
            builder.append(" WITH COLUMN GROUP (").append(this.columnGroupElements.stream()
                    .map(ColumnGroupElement::toString).collect(Collectors.joining(", "))).append(")");
        }
        if (this.viewOptions != null) {
            builder.append(" ").append(this.viewOptions);
        }
        builder.append(" AS ").append(this.asSelect);
        if (this.withOption != null) {
            builder.append(" ").append(this.withOption);
        }
        return builder.toString();
    }

}
