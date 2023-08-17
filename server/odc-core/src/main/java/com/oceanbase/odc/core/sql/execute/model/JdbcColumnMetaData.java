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
package com.oceanbase.odc.core.sql.execute.model;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.OdcConstants;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wenniu.ly
 * @date 2021/8/30
 */
@Data
@NoArgsConstructor
public class JdbcColumnMetaData {
    private boolean autoIncrement;
    private boolean caseSensitive;
    private boolean searchable;
    private boolean currency;
    /**
     * <pre>
     * 0 indicates that a column does not allow <code>NULL</code> values.
     * 1 indicates that a column allows <code>NULL</code> values.
     * 2 indicates that the nullability of a column's values is unknown.
     * </pre>
     */
    private int nullable;
    private boolean signed;
    private int columnDisplaySize;
    private String columnLabel;
    private String columnName;
    private String schemaName;
    private int precision;
    private int scale;
    private String tableName;
    private String catalogName;
    /**
     * Indicates the designated column's SQL type.
     *
     * @return SQL type from java.sql.Types
     * @see Types
     */
    private int columnType;
    private String columnTypeName;
    private boolean readOnly;
    private boolean writable;
    private boolean definitelyWritable;
    private String columnClassName;

    /**
     * below ODC additional meta
     */
    private String columnComment;

    /**
     * mark ODC internal column for special cases, <br>
     * sql console result-set viewer may hide these internal rows
     */
    private boolean internal = false;

    /**
     * indicate whether this column can be editable in result set
     */
    private boolean editable = false;

    /**
     * indicate whether this column is masked
     */
    private boolean masked = false;

    public JdbcColumnMetaData(ResultSetMetaData resultSetMetaData, int index) throws SQLException {
        this.autoIncrement = resultSetMetaData.isAutoIncrement(index);
        this.caseSensitive = resultSetMetaData.isCaseSensitive(index);
        this.searchable = resultSetMetaData.isSearchable(index);
        this.currency = resultSetMetaData.isCurrency(index);
        this.nullable = resultSetMetaData.isNullable(index);
        this.signed = resultSetMetaData.isSigned(index);
        this.columnDisplaySize = resultSetMetaData.getColumnDisplaySize(index);
        this.columnLabel = resultSetMetaData.getColumnLabel(index);
        this.columnName = resultSetMetaData.getColumnName(index);
        this.schemaName = resultSetMetaData.getSchemaName(index);
        this.precision = resultSetMetaData.getPrecision(index);
        this.scale = resultSetMetaData.getScale(index);
        this.tableName = resultSetMetaData.getTableName(index);
        this.catalogName = resultSetMetaData.getCatalogName(index);
        this.columnType = resultSetMetaData.getColumnType(index);
        this.columnTypeName = resultSetMetaData.getColumnTypeName(index);
        this.readOnly = resultSetMetaData.isReadOnly(index);
        this.writable = resultSetMetaData.isWritable(index);
        this.definitelyWritable = resultSetMetaData.isDefinitelyWritable(index);
        this.columnClassName = resultSetMetaData.getColumnClassName(index);

        if (StringUtils.equals(OdcConstants.ODC_INTERNAL_ROWID, this.columnName)) {
            this.internal = true;
        }
    }

    public String schemaName() {
        if (StringUtils.isNotBlank(this.schemaName)) {
            return this.schemaName;
        }
        return this.catalogName;
    }
}
