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

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link OutOfLineIndex}
 *
 * @author yh263208
 * @date 2022-12-26 14:57
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class OutOfLineIndex extends BaseStatement implements TableElement {

    private IndexOptions indexOptions;
    private Partition partition;
    private boolean fullText;
    private boolean spatial;
    private final String indexName;
    private final List<SortColumn> columns;
    private List<ColumnGroupElement> columnGroupElements;

    public OutOfLineIndex(@NonNull ParserRuleContext context, String indexName,
            @NonNull List<SortColumn> columns) {
        super(context);
        this.indexName = indexName;
        this.columns = columns;
    }

    public OutOfLineIndex(String indexName, @NonNull List<SortColumn> columns) {
        this.indexName = indexName;
        this.columns = columns;
    }

    @Override
    public String toString() {
        StringBuilder builder;
        if (this.fullText) {
            builder = new StringBuilder("FULLTEXT KEY");
        } else if (this.spatial) {
            builder = new StringBuilder("SPATIAL KEY");
        } else {
            builder = new StringBuilder("INDEX");
        }
        if (this.indexName != null) {
            builder.append(" ").append(this.indexName);
        }
        builder.append("(")
                .append(this.columns.stream().map(SortColumn::toString)
                        .collect(Collectors.joining(",")))
                .append(")");
        if (this.indexOptions != null) {
            builder.append(" ").append(this.indexOptions.toString());
        }
        if (this.partition != null) {
            builder.append(" ").append(this.partition.toString());
        }
        if (this.columnGroupElements != null) {
            builder.append(" WITH COLUMN GROUP(")
                    .append(columnGroupElements.stream()
                            .map(ColumnGroupElement::toString).collect(Collectors.joining(",")))
                    .append(")");
        }
        return builder.toString();
    }

}
