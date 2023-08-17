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
import java.util.Set;

import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link DMLBuilder}
 *
 * @author yh263208
 * @date 2023-03-09 16:13
 * @since ODC_release_4.2.0
 */
public interface DMLBuilder {

    List<DataModifyUnit> getModifyUnits();

    String getTableName();

    String getSchema();

    Set<String> getDataTypeNamesAvoidInWhereClause();

    Set<String> getDataTypeNamesNeedUpload();

    SqlBuilder createSQLBuilder();

    String toSQLString(@NonNull DataValue dataValue);

    boolean containsPrimaryKeys();

    boolean containsUniqueKeys();

    boolean containsPrimaryKeyOrRowId();

    default String toSQLString(@NonNull String dataType, String value) {
        return toSQLString(DataValue.ofRawValue(value, dataType));
    }

    void appendWhereClause(DataModifyUnit unit, SqlBuilder sqlBuilder);

}
