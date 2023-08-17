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
package com.oceanbase.tools.sqlparser.statement.delete;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link MultiDelete}
 *
 * @author jingtian
 * @date 2023/4/18
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MultiDelete extends BaseStatement {
    private final List<DeleteRelation> deleteRelations;
    private final boolean hasUsing;
    private final List<FromReference> tableReferences;

    public MultiDelete(@NonNull ParserRuleContext ruleNode, List<DeleteRelation> deleteRelations,
            @NonNull boolean hasUsing,
            @NonNull List<FromReference> tableReferences) {
        super(ruleNode);
        this.deleteRelations = deleteRelations;
        this.hasUsing = hasUsing;
        this.tableReferences = tableReferences;
    }

    public MultiDelete(@NonNull List<DeleteRelation> deleteRelations, @NonNull boolean hasUsing,
            @NonNull List<FromReference> tableReferences) {
        this.deleteRelations = deleteRelations;
        this.hasUsing = hasUsing;
        this.tableReferences = tableReferences;
    }

    @Override
    public String toString() {
        return this.getText();
    }

}
