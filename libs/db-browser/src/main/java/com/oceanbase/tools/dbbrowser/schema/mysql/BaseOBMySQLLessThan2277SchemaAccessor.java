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
package com.oceanbase.tools.dbbrowser.schema.mysql;

import java.util.ArrayList;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.oceanbase.tools.dbbrowser.model.DBColumnTypeDisplay;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn.KeyType;
import com.oceanbase.tools.dbbrowser.model.MySQLConstants;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMapper;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/9/20 下午5:24
 * @Description: [OBMySQL whose version is less than 2.2.77 implementation. In 2.2.77,
 *               information_schema.columns does not contain GENERATION_EXPRESSION column, so this
 *               implementation skip fetch this column in query sql.]
 */
public abstract class BaseOBMySQLLessThan2277SchemaAccessor extends OBMySQLBetween2277And3XSchemaAccessor {

    public BaseOBMySQLLessThan2277SchemaAccessor(JdbcOperations jdbcOperations,
            DBSchemaAccessorSqlMapper sqlMapper) {
        super(jdbcOperations);
        this.sqlMapper = sqlMapper;
    }

    @Override
    protected RowMapper listTableRowMapper() {
        return (rs, romNum) -> {
            DBTableColumn tableColumn = new DBTableColumn();
            tableColumn.setSchemaName(rs.getString("TABLE_SCHEMA"));
            tableColumn.setTableName(rs.getString("TABLE_NAME"));
            tableColumn.setName(rs.getString(MySQLConstants.COL_COLUMN_NAME));
            tableColumn.setTypeName(rs.getString(MySQLConstants.COL_DATA_TYPE));
            String fullTypeName = rs.getString(MySQLConstants.COL_COLUMN_TYPE);
            tableColumn.setFullTypeName(fullTypeName);
            if (StringUtils.isNotBlank(tableColumn.getTypeName())
                    && (isTypeEnum(tableColumn.getTypeName()) || isTypeSet((tableColumn.getTypeName())))) {
                tableColumn.setEnumValues(DBSchemaAccessorUtil.parseEnumValues(fullTypeName));
            }
            DBColumnTypeDisplay columnTypeDisplay = DBColumnTypeDisplay.fromName(tableColumn.getTypeName());
            if (columnTypeDisplay.displayScale()) {
                tableColumn.setScale(rs.getInt(MySQLConstants.COL_NUMERIC_SCALE));
            }
            if (columnTypeDisplay.displayPrecision()) {
                if (Objects.nonNull(rs.getObject(MySQLConstants.COL_NUMERIC_SCALE))) {
                    tableColumn.setPrecision(rs.getLong(MySQLConstants.COL_NUMERIC_PRECISION));
                } else if (Objects.nonNull(rs.getObject(MySQLConstants.COL_DATETIME_SCALE))) {
                    tableColumn.setPrecision(rs.getLong(MySQLConstants.COL_DATETIME_SCALE));
                } else {
                    tableColumn.setPrecision(rs.getLong(MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH));
                }
            }
            tableColumn.setExtraInfo(rs.getString(MySQLConstants.COL_COLUMN_EXTRA));

            Long maxLength = rs.getLong(MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH);
            if (Objects.isNull(maxLength)) {
                tableColumn.setMaxLength(rs.getLong(MySQLConstants.COL_NUMERIC_PRECISION));
            } else {
                tableColumn.setMaxLength(maxLength);
            }
            tableColumn.setCharsetName(rs.getString(MySQLConstants.COL_CHARACTER_SET_NAME));
            tableColumn.setCollationName(rs.getString(MySQLConstants.COL_COLLATION_NAME));
            tableColumn.setComment(rs.getString(MySQLConstants.COL_COLUMN_COMMENT));
            tableColumn.fillDefaultValue(rs.getString(MySQLConstants.COL_COLUMN_DEFAULT));
            tableColumn.setNullable("YES".equalsIgnoreCase(rs.getString(MySQLConstants.COL_IS_NULLABLE)));
            tableColumn.setVirtual(StringUtils.isNotEmpty(tableColumn.getGenExpression()));
            tableColumn.setOrdinalPosition(rs.getInt(MySQLConstants.COL_ORDINAL_POSITION));
            String keyTypeName = rs.getString(MySQLConstants.COL_COLUMN_KEY);
            if (StringUtils.isNotBlank(keyTypeName)) {
                tableColumn.setKeyType(KeyType.valueOf(keyTypeName));
            }

            tableColumn.setTypeModifiers(new ArrayList<>());
            tableColumn.setZerofill(false);
            tableColumn.setUnsigned(false);
            for (String modifier : fullTypeName.toLowerCase().split("\\s+")) {
                switch (modifier) {
                    case "zerofill":
                        tableColumn.setZerofill(true);
                        tableColumn.getTypeModifiers().add("zerofill");
                        break;
                    case "unsigned":
                        tableColumn.setUnsigned(true);
                        tableColumn.getTypeModifiers().add("unsigned");
                        break;
                }
            }

            if (Objects.nonNull(tableColumn.getExtraInfo())) {
                tableColumn.setAutoIncrement(
                        tableColumn.getExtraInfo().equalsIgnoreCase(MySQLConstants.EXTRA_AUTO_INCREMENT));
                tableColumn.setOnUpdateCurrentTimestamp(
                        tableColumn.getExtraInfo()
                                .equalsIgnoreCase(MySQLConstants.EXTRA_ON_UPDATE_CURRENT_TIMESTAMP));
                tableColumn.setStored(tableColumn.getExtraInfo()
                        .equalsIgnoreCase(MySQLConstants.EXTRA_STORED_GENERATED));
            }
            return tableColumn;
        };
    }

    @Override
    protected String getListTableColumnsSql(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append(
                "select TABLE_NAME, TABLE_SCHEMA, ORDINAL_POSITION, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, NUMERIC_SCALE, "
                        + "NUMERIC_PRECISION, "
                        + "DATETIME_PRECISION, CHARACTER_MAXIMUM_LENGTH, EXTRA, CHARACTER_SET_NAME, "
                        + "COLLATION_NAME, COLUMN_COMMENT, COLUMN_DEFAULT, IS_NULLABLE, "
                        + "COLUMN_KEY from information_schema.columns where TABLE_SCHEMA = ");
        sb.value(schemaName);
        sb.append(" ORDER BY TABLE_NAME, ORDINAL_POSITION");
        return sb.toString();
    }

}
