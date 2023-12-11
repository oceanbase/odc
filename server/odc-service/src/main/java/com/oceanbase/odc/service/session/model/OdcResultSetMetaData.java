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

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.core.sql.execute.model.JdbcColumnMetaData;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/8/30
 */
@Deprecated
@Data
public class OdcResultSetMetaData {

    private List<JdbcColumnMetaData> fieldMetaDataList;
    private boolean editable;
    // only if editable is true, table and columnList will be not null
    // may table or view
    private OdcTable table;
    protected List<DBTableColumn> dbColumnList;

    /**
     * @author mogao.zj
     */
    @Deprecated
    @Data
    public static class OdcTable {

        private String tableName;
        private String databaseName;
        private String character;
        private String collation;
        private String comment;
        private String tableSize;
        private String ddlSql;
        private boolean isPartitioned = false;
        private Long incrementValue;
        private DBObjectType type;
        private String tenantName;

        public static OdcTable of(String databaseName, String tableName) {
            OdcTable table = new OdcTable();
            table.setDatabaseName(databaseName);
            table.setTableName(tableName);
            return table;
        }

        public static OdcTable of(String databaseName, String tableName, String tenantName) {
            OdcTable table = new OdcTable();
            table.setDatabaseName(databaseName);
            table.setTableName(tableName);
            table.setTenantName(tenantName);
            return table;
        }

        public static OdcTable of(TableIdentity tableIdentity) {
            PreConditions.notNull(tableIdentity, "tableIdentity");
            return of(tableIdentity.getSchemaName(), tableIdentity.getTableName());
        }
    }

}

