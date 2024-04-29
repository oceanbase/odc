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
package com.oceanbase.tools.sqlparser.statement.createindex;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link CreateIndex}
 *
 * @author yh263208
 * @date 2023-06-02 13:55
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CreateIndex extends BaseStatement {

    private boolean fullText;
    private boolean spatial;
    private boolean unique;
    private boolean ifNotExists;
    private RelationFactor on;
    private RelationFactor relation;
    private IndexOptions indexOptions;
    private Partition partition;
    private final List<SortColumn> columns;
    private List<ColumnGroupElement> columnGroupElements;

    public CreateIndex(@NonNull ParserRuleContext context,
            @NonNull RelationFactor relation, @NonNull RelationFactor on,
            @NonNull List<SortColumn> columns) {
        super(context);
        this.on = on;
        this.relation = relation;
        this.columns = columns;
    }

    public CreateIndex(@NonNull RelationFactor relation, @NonNull RelationFactor on,
            @NonNull List<SortColumn> columns) {
        this.on = on;
        this.relation = relation;
        this.columns = columns;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("CREATE");
        if (this.unique) {
            builder.append(" UNIQUE");
        }
        if (this.fullText) {
            builder.append(" FULLTEXT");
        }
        if (this.spatial) {
            builder.append(" SPATIAL");
        }
        builder.append(" INDEX");
        if (this.ifNotExists) {
            builder.append(" IF NOT EXISTS");
        }
        builder.append(" ").append(this.relation)
                .append(" ON ").append(this.on)
                .append(" (\n\t").append(this.columns.stream()
                        .map(SortColumn::toString).collect(Collectors.joining(",\n\t")))
                .append("\n)");
        if (this.indexOptions != null) {
            builder.append(" ").append(this.indexOptions);
        }
        if (this.partition != null) {
            builder.append("\n").append(this.partition);
        }
        if (this.columnGroupElements != null) {
            builder.append(" WITH COLUMN GROUP(")
                    .append(columnGroupElements.stream().map(ColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        return builder.toString();
    }

}
