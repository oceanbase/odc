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
package com.oceanbase.tools.sqlparser.statement.droptable;

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 17:17
 * @Description: []
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DropTable extends BaseStatement {
    private TableList tableList;
    private final List<RelationFactor> relations;

    public DropTable(@NonNull ParserRuleContext context, @NonNull TableList tableList) {
        super(context);
        this.tableList = tableList;
        this.relations = tableList.getRelations();
    }

    public DropTable(@NonNull ParserRuleContext context, @NonNull RelationFactor relation) {
        super(context);
        this.relations = Collections.singletonList(relation);
    }

    @Override
    public String toString() {
        return this.getText();
    }
}
