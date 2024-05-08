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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link UpdateGenerator}
 *
 * @author yh263208
 * @date 2023-03-09 14:54
 * @since ODC_release_4.2.0
 * @see
 */
public class UpdateGenerator implements DMLGenerator {

    private final DMLBuilder dmlBuilder;
    private final Map<String, DBTableColumn> columnName2Column;
    private boolean affectMultiRows;

    public UpdateGenerator(@NonNull DMLBuilder dmlBuilder, @NonNull Map<String, DBTableColumn> columnName2Column) {
        this.dmlBuilder = dmlBuilder;
        this.columnName2Column = columnName2Column;
    }

    @Override
    public String generate() {
        List<DataModifyUnit> modifyUnits = this.dmlBuilder.getModifyUnits();
        SqlBuilder newBuilder = this.dmlBuilder.createSQLBuilder();
        SqlBuilder oldBuilder = this.dmlBuilder.createSQLBuilder();
        boolean hasBasicType = false;
        for (DataModifyUnit unit : modifyUnits) {
            if (unit.isUseDefault() || !StringUtils.equals(unit.getOldData(), unit.getNewData())) {
                newBuilder.identifier(unit.getColumnName()).append(" = ");
                if (unit.isUseDefault()) {
                    DBTableColumn tableColumn = columnName2Column.get(unit.getColumnName());
                    checkIfCanSetDefaultValue(tableColumn);
                    unit.setNewData("DEFAULT");
                } else if (Objects.nonNull(unit.getNewData())) {
                    unit.setNewData(this.dmlBuilder.toSQLString(unit.getNewDataValue()));
                }
                newBuilder.append(unit.getNewData()).append(", ");
            }

            // where condition, ignore null column
            if (!StringUtils.isEmpty(unit.getOldData())) {
                this.dmlBuilder.appendWhereClause(unit, oldBuilder);
                if (!this.dmlBuilder.containsPrimaryKeyOrRowId() && !this.dmlBuilder.containsUniqueKeys()) {
                    hasBasicType = true;
                }
            }
        }
        if (!this.dmlBuilder.containsPrimaryKeyOrRowId() && !this.dmlBuilder.containsUniqueKeys()) {
            PreConditions.validArgumentState(hasBasicType, ErrorCodes.ObBasicTypeColumnRequired, null,
                    "WITHOUT_BASIC_TYPE_COLUMN");
        }
        validateNewBuilderAndOldBuilder(oldBuilder, newBuilder);
        String oldSql = oldBuilder.substring(0, oldBuilder.length() - 5);
        String newSql = newBuilder.substring(0, newBuilder.length() - 2);
        SqlBuilder sqlBuilder = this.dmlBuilder.createSQLBuilder();
        sqlBuilder.append("update ")
                .identifier(this.dmlBuilder.getSchema(), this.dmlBuilder.getTableName())
                .append(" set ")
                .append(newSql)
                .append(" where ")
                .append(oldSql)
                .append(";");
        this.affectMultiRows = !this.dmlBuilder.containsUniqueKeys() && !this.dmlBuilder.containsPrimaryKeyOrRowId();
        return sqlBuilder.toString();
    }

    @Override
    public boolean isAffectMultiRows() {
        return this.affectMultiRows;
    }

    private void checkIfCanSetDefaultValue(DBTableColumn tableColumn) {
        if (tableColumn != null) {
            if (tableColumn.getDefaultValue() == null && tableColumn.getNullable() != null
                    && !tableColumn.getNullable()) {
                throw new BadRequestException("Can't set null value to a non-null column");
            }
        }
    }

    private void validateNewBuilderAndOldBuilder(SqlBuilder oldBuilder, SqlBuilder newBuilder) {
        PreConditions.validRequestState(oldBuilder.length() > 5, ErrorCodes.BadRequest, null,
                "Cannot generate where condition, at least one unique key collection should offered");
        PreConditions.validRequestState(newBuilder.length() > 2, ErrorCodes.BadRequest, null,
                "Cannot generate update DML, at least one column should be updated");
    }

}
