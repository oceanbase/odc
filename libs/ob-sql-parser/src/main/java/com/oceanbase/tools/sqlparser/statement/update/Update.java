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
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Update}
 *
 * @author yh263208
 * @date 2022-12-20 15:53
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Update extends BaseStatement {

    private boolean cursor;
    private Expression where;
    private OrderBy orderBy;
    private Limit limit;
    private final List<FromReference> tableReferences;
    private final List<UpdateAssign> assignList;

    public Update(@NonNull ParserRuleContext context, @NonNull List<FromReference> tableReferences,
            @NonNull List<UpdateAssign> assignList) {
        super(context);
        this.tableReferences = tableReferences;
        this.assignList = assignList;
    }

    public Update(@NonNull List<FromReference> tableReferences, @NonNull List<UpdateAssign> assignList) {
        this.tableReferences = tableReferences;
        this.assignList = assignList;
    }

    @Override
    public String toString() {
        return this.getText();
    }

}
