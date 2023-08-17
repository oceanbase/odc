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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link InsertGenerator}
 *
 * @author yh263208
 * @date 2023-03-09 16:41
 * @since ODC_release_4.2.0
 * @see DMLGenerator
 */
public class InsertGenerator implements DMLGenerator {

    private final DMLBuilder dmlBuilder;

    public InsertGenerator(@NonNull DMLBuilder dmlBuilder) {
        this.dmlBuilder = dmlBuilder;
    }

    @Override
    public String generate() {
        List<DataModifyUnit> modifyUnits = dmlBuilder.getModifyUnits();
        Validate.notEmpty(dmlBuilder.getModifyUnits(), "DataModifyUnit can not be empty");
        List<String> values = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        for (DataModifyUnit unit : modifyUnits) {
            columnNames.add(unit.getColumnName());
            if (unit.isUseDefault()) {
                unit.setNewData("DEFAULT");
            } else if (Objects.isNull(unit.getNewData())) {
                unit.setNewData("NULL");
            } else if (StringUtils.isEmpty(unit.getNewData())) {
                unit.setNewData("''");
            } else {
                unit.setNewData(dmlBuilder.toSQLString(unit.getNewDataValue()));
            }
            values.add(unit.getNewData());
        }
        SqlBuilder sqlBuilder = dmlBuilder.createSQLBuilder();
        sqlBuilder.append("insert into ")
                .identifier(dmlBuilder.getSchema(), dmlBuilder.getTableName())
                .append("(")
                .identifiers(columnNames)
                .append(") values(")
                .list(values)
                .append(");");
        return sqlBuilder.toString();
    }

    @Override
    public boolean isAffectMultiRows() {
        return false;
    }

}
