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
package com.oceanbase.tools.sqlparser.statement.update;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link UpdateAssign}
 *
 * @author jingtian
 * @date 2023/4/27
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class UpdateAssign extends BaseStatement {
    private final List<ColumnReference> leftList;
    private final Expression right;
    private final boolean useDefault;

    public UpdateAssign(@NonNull ParserRuleContext ruleNode, @NonNull List<ColumnReference> leftList, Expression right,
            boolean useDefault) {
        super(ruleNode);
        this.leftList = leftList;
        this.right = right;
        this.useDefault = useDefault;
    }

    public UpdateAssign(@NonNull List<ColumnReference> leftList, Expression right, boolean useDefault) {
        this.leftList = leftList;
        this.right = right;
        this.useDefault = useDefault;
    }

    @Override
    public String toString() {
        return this.getText();
    }
}
