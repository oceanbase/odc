/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.tools.sqlparser.statement.common;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/28
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class ColumnGroup extends BaseStatement {

    private final List<ColumnGroupElement> columnGroupElements;

    public ColumnGroup(ParserRuleContext ruleNode, List<ColumnGroupElement> columnGroupElements) {
        super(ruleNode);
        this.columnGroupElements = columnGroupElements;
    }

    public ColumnGroup(List<ColumnGroupElement> columnGroupElements) {
        this.columnGroupElements = columnGroupElements;
    }

    @Override
    public String toString() {
        return " COLUMN GROUP(" +
                columnGroupElements.stream().map(ColumnGroupElement::toString).collect(Collectors.joining(","))
                + ")";
    }

}
