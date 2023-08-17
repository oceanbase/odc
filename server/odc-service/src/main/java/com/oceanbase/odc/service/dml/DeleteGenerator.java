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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link DeleteGenerator}
 *
 * @author yh263208
 * @date 2023-03-09 14:46
 * @since ODC_release_4.2.0
 */
public class DeleteGenerator implements DMLGenerator {

    private final DMLBuilder dmlBuilder;
    private boolean affectMultiRows;

    public DeleteGenerator(@NonNull DMLBuilder dmlBuilder) {
        this.dmlBuilder = dmlBuilder;
    }

    @Override
    public String generate() {
        List<DataModifyUnit> modifyUnits = this.dmlBuilder.getModifyUnits();
        SqlBuilder sqlBuilder = this.dmlBuilder.createSQLBuilder();
        sqlBuilder.append("delete from ")
                .identifier(this.dmlBuilder.getSchema(), this.dmlBuilder.getTableName());

        SqlBuilder conditionBuilder = this.dmlBuilder.createSQLBuilder();
        for (DataModifyUnit dataModifyUnit : modifyUnits) {
            // Ignore null column
            if (!StringUtils.isEmpty(dataModifyUnit.getOldData())) {
                this.dmlBuilder.appendWhereClause(dataModifyUnit, conditionBuilder);
            }
        }
        validateConditionBuilder(conditionBuilder);
        String condition = conditionBuilder.substring(0, conditionBuilder.length() - 5);
        if (!condition.isEmpty()) {
            sqlBuilder.append(" where ").append(condition);
        }
        sqlBuilder.append(";");
        this.affectMultiRows = !this.dmlBuilder.containsUniqueKeys() && !this.dmlBuilder.containsPrimaryKeyOrRowId();
        return sqlBuilder.toString();
    }

    @Override
    public boolean isAffectMultiRows() {
        return this.affectMultiRows;
    }

    private void validateConditionBuilder(SqlBuilder conditionBuilder) {
        PreConditions.validRequestState(conditionBuilder.length() > 5, ErrorCodes.BadRequest, null,
                "Cannot generate where condition, at least one unique key collection should offered");
    }

}
