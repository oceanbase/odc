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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link NameReference}
 *
 * @author yh263208
 * @date 2022-11-24 22:18
 * @see FromReference
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class NameReference extends BaseStatement implements FromReference {

    private final String schema;
    private final String relation;
    private final String alias;
    @Setter
    private String userVariable;
    @Setter
    private PartitionUsage partitionUsage;
    @Setter
    private FlashbackUsage flashbackUsage;
    @Setter
    private Pivot pivot;
    @Setter
    private UnPivot unPivot;

    public NameReference(@NonNull ParserRuleContext context,
            String schema, @NonNull String relation, String alias) {
        super(context);
        this.schema = schema;
        this.relation = relation;
        this.alias = alias;
    }

    public NameReference(@NonNull TerminalNode context,
            String schema, @NonNull String relation, String alias) {
        super(context);
        this.schema = schema;
        this.relation = relation;
        this.alias = alias;
    }

    public NameReference(String schema,
            @NonNull String relation, String alias) {
        this.schema = schema;
        this.relation = relation;
        this.alias = alias;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (this.schema != null) {
            buffer.append(this.schema).append(".");
        }
        buffer.append(this.relation);
        if (this.userVariable != null) {
            buffer.append(this.userVariable);
        }
        if (this.partitionUsage != null) {
            buffer.append(" ").append(this.partitionUsage.toString());
        }
        if (this.flashbackUsage != null) {
            buffer.append(" ").append(this.flashbackUsage.toString());
        }
        if (this.alias != null) {
            buffer.append(" ").append(this.alias);
        }
        if (this.pivot != null) {
            buffer.append(" ").append(this.pivot.toString());
        }
        if (this.unPivot != null) {
            buffer.append(" ").append(this.unPivot.toString());
        }
        return buffer.toString();
    }

}
