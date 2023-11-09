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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ConstraintState}
 *
 * @author yh263208
 * @date 2023-05-19 18:03
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ConstraintState extends BaseStatement {

    private Boolean rely;
    private boolean usingIndexFlag;
    private IndexOptions indexOptions;
    private Partition partition;
    private Boolean enable;
    private Boolean validate;
    private Boolean enforced;

    public ConstraintState(@NonNull ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    public ConstraintState(TerminalNode terminalNode) {
        super(terminalNode);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.rely != null) {
            if (this.rely) {
                builder.append(" RELY");
            } else {
                builder.append(" NORELY");
            }
        }
        if (this.usingIndexFlag) {
            builder.append(" USING INDEX");
        }
        if (this.indexOptions != null) {
            builder.append(" ").append(this.indexOptions.toString());
        }
        if (this.partition != null) {
            builder.append(" ").append(this.partition.toString());
        }
        if (this.enable != null) {
            if (this.enable) {
                builder.append(" ENABLE");
            } else {
                builder.append(" DISABLE");
            }
        }
        if (this.validate != null) {
            if (this.validate) {
                builder.append(" VALIDATE");
            } else {
                builder.append(" NOVALIDATE");
            }
        }
        if (this.enforced != null) {
            builder.append(" ").append(this.enforced ? "ENFORCED" : "NOT ENFORCED");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
