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
package com.oceanbase.tools.sqlparser.statement.createmview;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.*;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 09:18
 * @since: 4.3.4
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MaterializedViewRefreshOption extends BaseStatement {

    private final boolean neverRefresh;
    private final String refreshMethod;
    private String refreshMode;
    private Expression next;
    private Expression startWith;

    public MaterializedViewRefreshOption(@NonNull ParserRuleContext context,
            boolean neverRefresh, String refreshMethod) {
        super(context);
        this.neverRefresh = neverRefresh;
        this.refreshMethod = refreshMethod;
    }

    public MaterializedViewRefreshOption(boolean neverRefresh, String refreshMethod) {
        this.neverRefresh = neverRefresh;
        this.refreshMethod = refreshMethod;
    }

    @Override
    public String toString() {
        if (this.neverRefresh) {
            return "NEVER REFRESH";
        }
        StringBuilder builder = new StringBuilder("REFRESH ").append(this.refreshMethod);
        if (this.refreshMode != null) {
            builder.append(" ON ").append(this.refreshMode);
        }
        if (this.startWith != null) {
            builder.append(" START WITH ").append(this.startWith);
        }
        if (this.next != null) {
            builder.append(" NEXT ").append(this.next);
        }
        return builder.toString();
    }

}
