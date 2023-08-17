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
package com.oceanbase.tools.sqlparser.statement.alter.table;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link RenameTableAction}
 *
 * @author yh263208
 * @date 2023-06-15 16:18
 * @since ODC_release_4.2.0
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class RenameTableAction extends BaseStatement {

    private final RelationFactor from;
    private final RelationFactor to;

    public RenameTableAction(@NonNull ParserRuleContext context,
            @NonNull RelationFactor from, @NonNull RelationFactor to) {
        super(context);
        this.from = from;
        this.to = to;
    }

    public RenameTableAction(@NonNull RelationFactor from,
            @NonNull RelationFactor to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return this.from + " TO " + this.to;
    }

}
