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
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.common.BaseOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link IndexOptions}
 *
 * @author yh263208
 * @date 2023-05-24 17:53
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class IndexOptions extends BaseOptions {

    private Integer virtualColumnId;
    private Integer blockSize;
    private Integer dataTableId;
    private Integer indexTableId;
    private Boolean global;
    private Integer maxUsedPartId;
    private String comment;
    private Boolean withRowId;
    private String withParser;
    private Boolean usingBtree;
    private Boolean usingHash;
    private Boolean visible;
    private Integer parallel;
    private Boolean noParallel;
    private Integer pctFree;
    private Integer pctUsed;
    private Integer iniTrans;
    private Integer maxTrans;
    private List<String> storage;
    private String tableSpace;
    private Boolean reverse;
    private List<ColumnReference> storing;
    private List<ColumnReference> ctxcat;
    private Integer keyBlockSize;
    private Map<String, String> vectorIndexParams;

    public IndexOptions(@NonNull ParserRuleContext context) {
        super(context);
    }

    public IndexOptions(@NonNull TerminalNode terminalNode) {
        super(terminalNode);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (Boolean.TRUE.equals(this.usingBtree)) {
            builder.append(" USING BTREE");
        }
        if (Boolean.TRUE.equals(this.usingHash)) {
            builder.append(" USING HASH");
        }
        if (this.global != null) {
            builder.append(" ").append(this.global ? "GLOBAL" : "LOCAL");
        }
        if (this.blockSize != null) {
            builder.append(" ").append("BLOCK_SIZE=").append(this.blockSize);
        }
        if (this.dataTableId != null) {
            builder.append(" ").append("DATA_TABLE_ID=").append(this.dataTableId);
        }
        if (this.indexTableId != null) {
            builder.append(" ").append("INDEX_TABLE_ID=").append(this.indexTableId);
        }
        if (this.virtualColumnId != null) {
            builder.append(" ").append("VIRTUAL_COLUMN_ID=").append(this.virtualColumnId);
        }
        if (this.maxUsedPartId != null) {
            builder.append(" ").append("MAX_USED_PART_ID=").append(this.maxUsedPartId);
        }
        if (this.comment != null) {
            builder.append(" ").append("COMMENT ").append(this.comment);
        }
        if (this.storing != null) {
            builder.append(" ").append("STORING(").append(this.storing.stream()
                    .map(ColumnReference::toString).collect(Collectors.joining(","))).append(")");
        }
        if (this.ctxcat != null) {
            builder.append(" ").append("CTXCAT(").append(this.ctxcat.stream()
                    .map(ColumnReference::toString).collect(Collectors.joining(","))).append(")");
        }
        if (Boolean.TRUE.equals(this.withRowId)) {
            builder.append(" WITH ROWID");
        }
        if (this.withParser != null) {
            builder.append(" WITH PARSER ").append(this.withParser);
        }
        if (this.visible != null) {
            builder.append(" ").append(this.visible ? "VISIBLE" : "INVISIBLE");
        }
        if (this.parallel != null) {
            builder.append(" PARALLEL=").append(this.parallel);
        }
        if (Boolean.TRUE.equals(this.noParallel)) {
            builder.append(" NOPARALLEL");
        }
        if (Boolean.TRUE.equals(this.reverse)) {
            builder.append(" REVERSE");
        }
        if (this.pctFree != null) {
            builder.append(" PCTFREE=").append(this.pctFree);
        }
        if (this.pctUsed != null) {
            builder.append(" PCTUSED ").append(this.pctUsed);
        }
        if (this.iniTrans != null) {
            builder.append(" INITRANS ").append(this.iniTrans);
        }
        if (this.maxTrans != null) {
            builder.append(" MAXTRANS ").append(this.maxTrans);
        }
        if (this.storage != null) {
            builder.append(" STORAGE(").append(String.join(" ", this.storage)).append(")");
        }
        if (this.tableSpace != null) {
            builder.append(" TABLESPACE ").append(this.tableSpace);
        }
        if (this.keyBlockSize != null) {
            builder.append(" KEY_BLOCK_SIZE ").append(this.keyBlockSize);
        }
        if (this.vectorIndexParams != null) {
            builder.append(" WITH (").append(this.vectorIndexParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "))).append(")");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
