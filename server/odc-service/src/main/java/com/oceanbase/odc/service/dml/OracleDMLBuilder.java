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
import java.util.Objects;
import java.util.Set;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link OracleDMLBuilder}
 *
 * @author yh263208
 * @date 2023-03-07 17:17
 * @since ODC_release_4.2.0
 * @see BaseDMLBuilder
 */
public class OracleDMLBuilder extends BaseDMLBuilder {

    public OracleDMLBuilder(@NonNull List<DataModifyUnit> modifyUnits, List<String> whereColumns,
            ConnectionSession connectionSession) {
        super(modifyUnits, whereColumns, connectionSession);
    }

    @Override
    public Set<String> getDataTypeNamesAvoidInWhereClause() {
        return getDataTypeNamesNeedUpload();
    }

    @Override
    public Set<String> getDataTypeNamesNeedUpload() {
        return new HashSet<>(Arrays.asList("blob", "clob", "raw"));
    }

    @Override
    public SqlBuilder createSQLBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    public String toSQLString(@NonNull DataValue dataValue) {
        return DataConvertUtil.convertToSqlString(connectionSession, dataValue);
    }

    @Override
    protected boolean isAppendable(DataModifyUnit unit) {
        if (containsRowId()) {
            return OdcConstants.ROWID.equalsIgnoreCase(unit.getColumnName());
        } else if (containsODCInternalRowId()) {
            return OdcConstants.ODC_INTERNAL_ROWID.equalsIgnoreCase(unit.getColumnName());
        }
        return super.isAppendable(unit);
    }

    private boolean containsRowId() {
        return getModifyUnits().stream().anyMatch(t -> OdcConstants.ROWID.equalsIgnoreCase(t.getColumnName())
                && Objects.nonNull(t.getOldData()));
    }

    private boolean containsODCInternalRowId() {
        return getModifyUnits().stream()
                .anyMatch(t -> OdcConstants.ODC_INTERNAL_ROWID.equalsIgnoreCase(t.getColumnName())
                        && Objects.nonNull(t.getOldData()));
    }

    @Override
    public boolean containsPrimaryKeyOrRowId() {
        return super.containsPrimaryKeyOrRowId() || containsRowId() || containsODCInternalRowId();
    }

    @Override
    protected String preHandleColumnName(String columnName) {
        if (OdcConstants.ODC_INTERNAL_ROWID.equalsIgnoreCase(columnName)) {
            return OdcConstants.ROWID;
        }
        return columnName;
    }

}
