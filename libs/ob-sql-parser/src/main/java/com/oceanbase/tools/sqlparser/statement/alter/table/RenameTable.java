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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link RenameTable}
 *
 * @author yh263208
 * @date 2023-06-15 16:48
 * @since ODC_release_4.2.0
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class RenameTable extends BaseStatement {

    private final List<RenameTableAction> actions;

    public RenameTable(@NonNull ParserRuleContext context,
            @NonNull List<RenameTableAction> actions) {
        super(context);
        this.actions = actions;
    }

    public RenameTable(@NonNull List<RenameTableAction> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "RENAME TABLE " + this.actions.stream()
                .map(RenameTableAction::toString).collect(Collectors.joining(","));
    }

}
