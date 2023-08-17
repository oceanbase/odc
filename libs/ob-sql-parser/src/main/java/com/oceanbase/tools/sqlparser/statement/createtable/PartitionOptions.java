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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.BaseOptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link PartitionOptions}
 *
 * @author yh263208
 * @date 2023-05-30 14:04
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PartitionOptions extends BaseOptions {

    private Integer id;
    private Boolean noCompress;
    private String compress;
    private String engine;
    private Integer iniTrans;
    private Integer maxTrans;
    private Integer pctFree;
    private Integer pctUsed;
    private List<String> storage;
    private String tableSpace;

    public PartitionOptions(@NonNull ParserRuleContext context) {
        super(context);
    }

    public PartitionOptions(@NonNull TerminalNode node) {
        super(node);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.id != null) {
            builder.append(" ID ").append(this.id);
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
        if (this.engine != null) {
            builder.append(" ENGINE=").append(this.engine);
        }
        if (Boolean.TRUE.equals(this.noCompress)) {
            builder.append(" NOCOMPRESS");
        }
        if (this.compress != null) {
            builder.append(" COMPRESS ").append(getCompress());
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
