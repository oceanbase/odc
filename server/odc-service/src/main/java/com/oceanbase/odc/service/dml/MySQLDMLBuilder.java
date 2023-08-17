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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link MySQLDMLBuilder}
 *
 * @author yh263208
 * @date 2023-03-07 17:17
 * @since ODC_release_4.2.0
 * @see BaseDMLBuilder
 */
public class MySQLDMLBuilder extends BaseDMLBuilder {

    public MySQLDMLBuilder(@NonNull List<DataModifyUnit> modifyUnits, List<String> whereColumns,
            ConnectionSession connectionSession) {
        super(modifyUnits, whereColumns, connectionSession);
    }

    @Override
    public Set<String> getDataTypeNamesAvoidInWhereClause() {
        return new HashSet<>(Arrays.asList("text", "tinytext", "mediumtext", "longtext",
                "tinyblob", "blob", "mediumblob", "longblob", "bit", "binary", "varbinary"));
    }

    @Override
    public Set<String> getDataTypeNamesNeedUpload() {
        return new HashSet<>(Arrays.asList("tinyblob", "blob", "mediumblob", "longblob"));
    }

    @Override
    public SqlBuilder createSQLBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    public String toSQLString(@NonNull DataValue dataValue) {
        return DataConvertUtil.convertToSqlString(connectionSession, dataValue);
    }

}
