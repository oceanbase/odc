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
package com.oceanbase.tools.dbbrowser.model;

import java.util.List;

import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/5/10
 */
@Data
public class DBColumnGroupElement {
    private boolean allColumns;
    private boolean eachColumn;
    private String groupName;
    private List<String> columnNames;

    public static DBColumnGroupElement ofColumnGroupElement(ColumnGroupElement e) {
        DBColumnGroupElement group = new DBColumnGroupElement();
        if (e.isAllColumns()) {
            group.setAllColumns(true);
        } else if (e.isEachColumn()) {
            group.setEachColumn(true);
        } else {
            group.setGroupName(e.getGroupName());
            group.setColumnNames(e.getColumnNames());
        }
        return group;
    }

    @Override
    public String toString() {
        if (isAllColumns()) {
            return "all columns";
        } else if (isEachColumn()) {
            return "each column";
        }
        return String.format("%s(%s)", groupName, String.join(",", columnNames));
    }

}
