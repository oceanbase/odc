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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.datamasking.algorithm.Algorithm;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualTable;
import com.oceanbase.odc.core.sql.execute.model.JdbcColumnMetaData;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.DataMaskingUtil;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.interceptor.BaseTimeConsumingInterceptor;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.DBResultSetMetaData;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/6/12 14:01
 */
@Slf4j
@Component
public class DataMaskingInterceptor extends BaseTimeConsumingInterceptor {

    @Autowired
    private DataMaskingService maskingService;

    @Override
    public boolean preHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) {
        return true;
    }

    @Override
    @SuppressWarnings("all")
    public void doAfterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull AsyncExecuteContext context) throws Exception {
        // TODO: May intercept sensitive column operation (WHERE / ORDER BY / HAVING)
        if (!maskingService.isMaskingEnabled()) {
            return;
        }
        if (response.getStatus() != SqlExecuteStatus.SUCCESS || response.getRows().isEmpty()) {
            return;
        }
        try {
            List<Set<SensitiveColumn>> resultSetSensitiveColumns =
                    maskingService.getResultSetSensitiveColumns(response.getExecuteSql(), session);
            if (!DataMaskingUtil.isSensitiveColumnExists(resultSetSensitiveColumns)) {
                return;
            }
            response.setExistSensitiveData(true);
            List<Algorithm> algorithms = maskingService.getResultSetMaskingAlgorithmMaskers(resultSetSensitiveColumns);
            maskingService.maskRowsUsingAlgorithms(response, algorithms);
            try {
                setQueryCache(response, session, algorithms);
            } catch (Exception e) {
                log.warn("Failed to set query cache after data masking, sql={}", response.getExecuteSql(), e);
            }
            try {
                setEditable(response, session, algorithms);
            } catch (Exception e) {
                log.warn("Failed to set editable after data masking, sql={}", response.getExecuteSql(), e);
            }
        } catch (Exception e) {
            // Eat exception and skip data masking
            log.warn("Failed to mask query result set in SQL console, sql={}", response.getExecuteSql(), e);
        }
    }

    @Override
    protected String getExecuteStageName() {
        return SqlExecuteStages.DATA_MASKING;
    }

    @Override
    public int getOrder() {
        return 5;
    }

    private void setEditable(SqlExecuteResult response, ConnectionSession session, List<Algorithm> algorithms) {
        if (!response.getResultSetMetaData().isEditable()) {
            return;
        }
        DBResultSetMetaData metaData = response.getResultSetMetaData();
        String schemaName = metaData.getTable().getDatabaseName();
        String tableName = metaData.getTable().getTableName();
        List<String> columns = response.getResultSetMetaData().getFieldMetaDataList().stream()
                .map(JdbcColumnMetaData::getColumnName).collect(Collectors.toList());
        Set<String> sensitiveColumns = new HashSet<>();
        for (int i = 0; i < columns.size(); i++) {
            if (Objects.nonNull(algorithms.get(i))) {
                sensitiveColumns.add(columns.get(i));
            }
        }
        String rowIdColumn = null;
        if (session.getDialectType().isOracle()) {
            for (String column : columns) {
                if (OdcConstants.ODC_INTERNAL_ROWID.equalsIgnoreCase(column)
                        || OdcConstants.ROWID.equalsIgnoreCase(column)) {
                    rowIdColumn = column;
                    break;
                }
            }
        }
        if (Objects.nonNull(rowIdColumn)) {
            response.setWhereColumns(Collections.singletonList(rowIdColumn));
            return;
        }

        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<DBTableConstraint> constraints = accessor.listTableConstraints(schemaName, tableName).stream()
                .filter(e -> e.getType() == DBConstraintType.PRIMARY_KEY || e.getType() == DBConstraintType.UNIQUE_KEY)
                .sorted(Comparator.comparingInt(o -> o.getType().ordinal()))
                .collect(Collectors.toList());
        for (DBTableConstraint constraint : constraints) {
            List<String> constraintColumns = constraint.getColumnNames();
            boolean containsSensitive = false;
            for (String constraintColumn : constraintColumns) {
                if (sensitiveColumns.contains(constraintColumn)) {
                    containsSensitive = true;
                    break;
                }
            }
            if (new HashSet<>(columns).containsAll(constraintColumns) && !containsSensitive) {
                response.setWhereColumns(constraintColumns);
                return;
            }
        }
        metaData.setEditable(false);
    }

    private void setQueryCache(SqlExecuteResult response, ConnectionSession session, List<Algorithm> algorithms) {
        VirtualTable virtualTable = ConnectionSessionUtil.getQueryCache(session, response.getSqlId());
        if (Objects.isNull(virtualTable)) {
            return;
        }
        List<Integer> maskedColumnIds = new ArrayList<>();
        List<Integer> existColumnIds = virtualTable.columnIds();
        for (int i = 0; i < algorithms.size(); i++) {
            if (Objects.isNull(algorithms.get(i)) && existColumnIds.contains(i)) {
                maskedColumnIds.add(i);
            }
        }
        VirtualTable maskedVirtualTable = virtualTable.project(maskedColumnIds, virtualElements -> virtualElements);
        ConnectionSessionUtil.setQueryCache(session, maskedVirtualTable, response.getSqlId());
    }

}
