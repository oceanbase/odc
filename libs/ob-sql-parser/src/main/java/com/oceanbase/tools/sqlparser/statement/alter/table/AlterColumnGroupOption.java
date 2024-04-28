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
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroup;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/28
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class AlterColumnGroupOption extends BaseStatement {
    private final boolean isAdd;
    private final List<ColumnGroup> columnGroups;

    public AlterColumnGroupOption(ParserRuleContext ruleNode, boolean isAdd, List<ColumnGroup> columnGroups) {
        super(ruleNode);
        this.isAdd = isAdd;
        this.columnGroups = columnGroups;
    }

    public AlterColumnGroupOption(boolean isAdd, List<ColumnGroup> columnGroups) {
        this.isAdd = isAdd;
        this.columnGroups = columnGroups;
    }

    @Override
    public String toString() {
        return (isAdd ? " ADD" : " DROP")
                + " COLUMN GROUP("
                + columnGroups.stream().map(ColumnGroup::toString).collect(Collectors.joining(","))
                + ")";
    }

}
