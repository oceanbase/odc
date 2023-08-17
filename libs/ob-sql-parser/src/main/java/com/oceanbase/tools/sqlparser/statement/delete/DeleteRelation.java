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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link DeleteRelation}
 *
 * @author jingtian
 * @date 2023/4/28
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DeleteRelation extends BaseStatement {
    private final String schema;
    private final String table;
    private final boolean useStar;

    public DeleteRelation(@NonNull ParserRuleContext ruleNode, String schema, @NonNull String table, boolean useStar) {
        super(ruleNode);
        this.schema = schema;
        this.table = table;
        this.useStar = useStar;
    }

    public DeleteRelation(String schema, @NonNull String table, boolean useStar) {
        this.schema = schema;
        this.table = table;
        this.useStar = useStar;
    }

    @Override
    public String toString() {
        return this.getText();
    }
}
