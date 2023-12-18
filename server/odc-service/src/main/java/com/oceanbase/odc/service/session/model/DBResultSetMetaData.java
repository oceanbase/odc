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
package com.oceanbase.odc.service.session.model;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.db.model.OdcDBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jingtian
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DBResultSetMetaData extends OdcResultSetMetaData {

    private List<OdcDBTableColumn> columnList;

    public DBResultSetMetaData() {}

    public void setColumnList(List<DBTableColumn> columnList) {
        if (columnList == null) {
            this.columnList = null;
            this.dbColumnList = null;
            return;
        }
        this.columnList = columnList.stream().map(OdcDBTableColumn::new).collect(Collectors.toList());
        this.dbColumnList = columnList;
    }

    @Override
    public void setDbColumnList(List<DBTableColumn> dbColumnList) {
        if (dbColumnList == null) {
            this.dbColumnList = null;
            this.columnList = null;
            return;
        }
        this.dbColumnList = dbColumnList;
        this.columnList = columnList.stream().map(OdcDBTableColumn::new).collect(Collectors.toList());
    }

}
