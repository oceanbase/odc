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
package com.oceanbase.odc.service.dml;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link BaseDMLBuilder}
 *
 * @author yh263208
 * @date 2023-03-07 15:35
 */
abstract class BaseDMLBuilder implements DMLBuilder {

    private final String tableName;
    private final String schema;
    private final List<DataModifyUnit> modifyUnits;
    private final List<DBTableConstraint> constraints;
    private final List<String> whereColumns;
    protected final ConnectionSession connectionSession;

    public BaseDMLBuilder(@NonNull List<DataModifyUnit> modifyUnits, List<String> whereColumns,
            @NonNull ConnectionSession connectionSession) {
        Set<String> schemas = modifyUnits.stream()
                .map(DataModifyUnit::getSchemaName)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (schemas.size() > 1) {
            throw new IllegalArgumentException("Multi schemas, count=" + schemas);
        }
        Set<String> tables = modifyUnits.stream()
                .map(DataModifyUnit::getTableName)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (tables.size() != 1) {
            throw new IllegalArgumentException("Multi tables, count=" + tables);
        }
        this.modifyUnits = modifyUnits;
        this.tableName = tables.stream().findFirst().orElse(null);
        if (schemas.size() == 1) {
            this.schema = schemas.stream().findFirst().orElse(null);
        } else {
            this.schema = null;
        }
        this.connectionSession = connectionSession;
        this.constraints = getConstraints(this.schema, this.tableName);
        this.whereColumns = whereColumns;
    }

    @Override
    public String getTableName() {
        return this.tableName;
    }

    @Override
    public String getSchema() {
        return this.schema;
    }

    @Override
    public List<DataModifyUnit> getModifyUnits() {
        return this.modifyUnits;
    }

    @Override
    public void appendWhereClause(DataModifyUnit unit, SqlBuilder sqlBuilder) {
        Set<String> avoids = getDataTypeNamesAvoidInWhereClause();
        DataTypeFactory factory = new CommonDataTypeFactory(unit.getColumnType());
        DataType dataType;
        try {
            dataType = factory.generate();
        } catch (IOException | SQLException e) {
            throw new IllegalStateException(e);
        }
        if (!isAppendable(unit) || avoids.contains(dataType.getDataTypeName())) {
            // 拼sql的条件表达式中，忽略binary、blob大对象类型
            return;
        }
        String columnName = preHandleColumnName(unit.getColumnName());
        if (isFloat(dataType.getDataTypeName())) {
            sqlBuilder.append("cast(").identifier(columnName).append(" as char)=");
        } else {
            sqlBuilder.identifier(columnName).append("=");
        }
        unit.setOldData(toSQLString(unit.getColumnType(), unit.getOldData()));
        sqlBuilder.append(unit.getOldData()).append(" and ");
    }

    @Override
    public boolean containsPrimaryKeyOrRowId() {
        return containsPrimaryKeys();
    }

    @Override
    public boolean containsPrimaryKeys() {
        return getPrimaryConstraint() != null;
    }

    @Override
    public boolean containsUniqueKeys() {
        final Set<String> columnNames = this.modifyUnits.stream()
                .map(DataModifyUnit::getColumnName).collect(Collectors.toSet());
        return this.constraints.stream()
                .filter(c -> c.getType() == DBConstraintType.UNIQUE_KEY && c.getColumnNames() != null)
                .anyMatch(c -> columnNames.containsAll(c.getColumnNames()));
    }

    protected boolean isAppendable(DataModifyUnit unit) {
        if (CollectionUtils.isNotEmpty(this.whereColumns)) {
            return whereColumns.contains(unit.getColumnName());
        }
        if (containsPrimaryKeys()) {
            DBTableConstraint primary = getPrimaryConstraint();
            return primary != null && primary.getColumnNames().contains(unit.getColumnName());
        } else if (containsUniqueKeys()) {
            return matchUniqueConstraint(unit);
        }
        return true;
    }

    protected String preHandleColumnName(String columnName) {
        return columnName;
    }

    private boolean isFloat(String type) {
        return StringUtils.containsIgnoreCase(type, "float")
                || StringUtils.containsIgnoreCase(type, "double")
                || StringUtils.containsIgnoreCase(type, "decimal");
    }

    private boolean matchUniqueConstraint(DataModifyUnit u) {
        Optional<DBTableConstraint> optional = this.constraints.stream()
                .filter(c -> c.getType() == DBConstraintType.UNIQUE_KEY).findFirst();
        return optional.isPresent() && optional.get().getColumnNames().contains(u.getColumnName());
    }

    private DBTableConstraint getPrimaryConstraint() {
        Map<String, DataModifyUnit> name2Units = this.modifyUnits.stream()
                .collect(Collectors.toMap(DataModifyUnit::getColumnName, unit -> unit));
        for (DBTableConstraint constraint : this.constraints) {
            if (DBConstraintType.PRIMARY_KEY != constraint.getType()) {
                continue;
            }
            for (String col : constraint.getColumnNames()) {
                if (!name2Units.containsKey(col) || name2Units.get(col).getOldData() == null) {
                    return null;
                }
            }
            return constraint;
        }
        return null;
    }

    private List<DBTableConstraint> getConstraints(String schema,
            @NonNull String tableName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        String schemaName = schema;
        if (schema == null) {
            schemaName = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        }
        return accessor.listTableConstraints(schemaName, tableName);
    }

}
