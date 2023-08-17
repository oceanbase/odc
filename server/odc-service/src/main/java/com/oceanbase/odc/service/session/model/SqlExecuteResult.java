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
package com.oceanbase.odc.service.session.model;

import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.github.benmanes.caffeine.cache.Cache;
import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.common.util.TraceWatch;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.core.sql.execute.model.JdbcColumnMetaData;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.common.model.OdcResultSetMetaData.OdcTable;
import com.oceanbase.odc.service.common.util.PLObjectErrMsgUtils;
import com.oceanbase.odc.service.feature.AllFeatures;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.parser.ParserUtil;
import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * sql execution result package
 *
 * @author yh263208
 * @date 2021-11-18 10:38
 * @since ODC_release_3.2.2
 */
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class SqlExecuteResult {
    private List<String> columnLabels;
    private List<String> columns;
    private SqlExecuteStatus status = SqlExecuteStatus.CREATED;
    private boolean connectionReset = false;
    private String sqlId;
    private String dbmsOutput;
    private String executeSql;
    private String originSql;
    private String sqlType;
    private GeneralSqlType generalSqlType;
    private DBResultSetMetaData resultSetMetaData;
    private List<List<Object>> rows = new LinkedList<>();
    private Integer total;
    private String traceId;
    private String track;
    private Map<String, Object> types;
    private Map<String, String> typeNames;
    private String dbObjectType;
    private String dbObjectName;
    private List<String> dbObjectNameList;
    private boolean existWarnings = false;
    @JsonProperty(access = Access.WRITE_ONLY)
    private TraceWatch traceWatch = new TraceWatch("Default");
    private List<CheckViolation> checkViolations = new ArrayList<>();
    private Boolean allowExport;
    private boolean existSensitiveData = false;
    private List<String> whereColumns;

    public static SqlExecuteResult emptyResult(@NonNull SqlTuple sqlTuple, @NonNull SqlExecuteStatus status) {
        SqlExecuteResult result = new SqlExecuteResult(sqlTuple.getExecutedSql(), sqlTuple.getSqlId());
        result.status = status;
        result.total = 0;
        result.originSql = sqlTuple.getOriginalSql();
        result.traceWatch = sqlTuple.getSqlWatch();
        return result;
    }

    public SqlExecuteResult(JdbcGeneralResult generalResult) {
        init(generalResult);
    }

    private SqlExecuteResult(String sql, String sqlId) {
        PreConditions.notNull(sql, "Sql");
        PreConditions.notNull(sqlId, "SqlId");
        this.status = SqlExecuteStatus.RUNNING;
        this.executeSql = sql;
        this.sqlId = sqlId;
    }

    public void initWarningMessage(ConnectionSession connectionSession) {
        if (connectionSession.getDialectType() != DialectType.OB_ORACLE || !this.existWarnings) {
            return;
        }
        try {
            this.track = PLObjectErrMsgUtils.getOraclePLObjErrMsg(connectionSession,
                    ConnectionSessionUtil.getCurrentSchema(connectionSession), this.dbObjectType, this.dbObjectName);
        } catch (Exception exception) {
            log.warn("Failed to get SQL execution warning information", exception);
        }
    }

    public OdcTable initEditableInfo() {
        boolean editable = true;
        OdcTable resultTable = null;
        Set<OdcTable> relatedTablesOrViews = new HashSet<>();
        if (Objects.isNull(this.resultSetMetaData)) {
            return null;
        }
        List<JdbcColumnMetaData> fieldMetaDataList = resultSetMetaData.getFieldMetaDataList();
        // if there are more than one column with same name, the result can't be edited
        List<String> columnNames = new ArrayList<>();
        for (JdbcColumnMetaData odcFieldMetaData : fieldMetaDataList) {
            String tableName = odcFieldMetaData.getTableName();
            // not editable if table not exists, may function
            if (StringUtils.isBlank(tableName)) {
                editable = false;
                break;
            }
            String schemaName = odcFieldMetaData.schemaName();
            if (StringUtils.isBlank(schemaName)) {
                editable = false;
                break;
            }

            OdcTable currentTableOrView = new OdcTable();
            currentTableOrView.setDatabaseName(schemaName);
            currentTableOrView.setTableName(tableName);
            relatedTablesOrViews.add(currentTableOrView);
            String columnName = odcFieldMetaData.getColumnName();
            if (columnNames.contains(columnName)) {
                editable = false;
                break;
            } else {
                columnNames.add(columnName);
            }
            if (Types.ROWID == odcFieldMetaData.getColumnType()) {
                // not editable if there is more than one RowId
                if (Objects.nonNull(resultTable)) {
                    editable = false;
                    break;
                }
                resultTable = currentTableOrView;
            }
        }
        // if there are more than one table and with no RowId, the result can't be edited
        if (CollectionUtils.isEmpty(relatedTablesOrViews)
                || (Objects.isNull(resultTable) && relatedTablesOrViews.size() > 1)) {
            editable = false;
        }

        if (!editable) {
            resultSetMetaData.setEditable(false);
            return null;
        }
        if (relatedTablesOrViews.size() == 1) {
            resultTable = relatedTablesOrViews.iterator().next();
        }

        resultSetMetaData.setEditable(editable);
        resultSetMetaData.setTable(resultTable);
        // set rows of the table with rowid editable
        for (JdbcColumnMetaData odcFieldMetaData : resultSetMetaData.getFieldMetaDataList()) {
            odcFieldMetaData.setEditable(
                    odcFieldMetaData.schemaName().equals(resultTable.getDatabaseName())
                            && odcFieldMetaData.getTableName().equals(resultTable.getTableName())
                            && Types.ROWID != odcFieldMetaData.getColumnType());
        }
        return resultTable;
    }

    public void initColumnInfo(@NonNull ConnectionSession connectionSession, OdcTable resultTable,
            @NonNull DBSchemaAccessor schemaAccessor) {
        if (this.resultSetMetaData == null) {
            return;
        }
        Map<OdcTable, List<String>> table2ColumnNames = new HashMap<>();
        List<DBTableColumn> resultColumnList = new ArrayList<>();
        // first get all table column comments and then get editable table column info
        // cared tables
        Set<TableIdentity> caredTables = new TreeSet<>();
        List<JdbcColumnMetaData> fields = this.resultSetMetaData.getFieldMetaDataList();
        for (JdbcColumnMetaData field : fields) {
            if (Objects.nonNull(field.schemaName()) && Objects.nonNull(field.getTableName())) {
                caredTables.add(TableIdentity.of(field.schemaName(), field.getTableName()));
            }
            OdcTable currentTableOrView = new OdcTable();
            currentTableOrView.setDatabaseName(field.schemaName());
            currentTableOrView.setTableName(field.getTableName());
            if (Types.ROWID == field.getColumnType()) {
                DBTableColumn column = new DBTableColumn();
                column.setTableName(field.getTableName());
                column.setName(field.getColumnName());
                column.setTypeName(OdcConstants.ROWID);
                resultColumnList.add(column);
            } else {
                table2ColumnNames
                        .computeIfAbsent(currentTableOrView, names -> new ArrayList<>()).add(field.getColumnName());
            }
        }
        // collect all cared column meta
        Map<ColumnIdentity, DBTableColumn> columnMap = new HashMap<>();
        for (TableIdentity table : caredTables) {
            try {
                List<DBTableColumn> columns = tryToGetTableColumnsFromCache(connectionSession, schemaAccessor, table);
                for (DBTableColumn column : columns) {
                    columnMap.put(ColumnIdentity.of(table, column.getName()), column);
                }
            } catch (Exception e) {
                log.warn("get column list failed, table={}, reason={}", table, e.getMessage());
            }
        }
        // attach column comment into field metadata
        fields = this.resultSetMetaData.getFieldMetaDataList();
        for (JdbcColumnMetaData field : fields) {
            if (Objects.nonNull(field.schemaName()) && Objects.nonNull(field.getTableName())
                    && Objects.nonNull(field.getColumnName())) {
                DBTableColumn column = columnMap
                        .get(ColumnIdentity.of(field.schemaName(), field.getTableName(), field.getColumnName()));
                if (Objects.nonNull(column)) {
                    field.setColumnComment(column.getComment());
                }
            }
        }
        // If the resultSet is not editable, do not query column information
        if (!resultSetMetaData.isEditable() || resultTable == null) {
            return;
        }
        // first assume a table related query, then assume a view related query
        // if neither, then not editable
        List<DBTableColumn> dbTableColumns = tryToGetTableColumnsFromCache(connectionSession, schemaAccessor,
                TableIdentity.of(resultTable.getDatabaseName(), resultTable.getTableName()));

        if (!CollectionUtils.isEmpty(dbTableColumns)) {
            for (DBTableColumn column : dbTableColumns) {
                if (table2ColumnNames.get(resultTable).contains(column.getName())) {
                    // columns order by table definitions
                    resultColumnList.add(column);
                }
            }
        } else if (AllFeatures.getByConnectType(connectionSession.getConnectType()).supportsViewObject()) {
            DBView dbView = tryToGetViewColumnsFromCache(connectionSession, schemaAccessor,
                    TableIdentity.of(resultTable.getDatabaseName(), resultTable.getTableName()));
            if (!CollectionUtils.isEmpty(dbView.getColumns())) {
                for (DBTableColumn column : dbView.getColumns()) {
                    if (table2ColumnNames.get(resultTable).contains(column.getName())) {
                        resultColumnList.add(column);
                    }
                }
            } else {
                log.info("Cannot retrieve neither table nor view info");
            }
        }
        resultSetMetaData.setColumnList(resultColumnList);
    }

    private List<DBTableColumn> tryToGetTableColumnsFromCache(@NonNull ConnectionSession connectionSession,
            @NonNull DBSchemaAccessor schemaAccessor, @NonNull TableIdentity table) {
        Cache<TableIdentity, List<DBTableColumn>> tableColumnsCache =
                ConnectionSessionUtil.getTableColumnCache(connectionSession);
        return tableColumnsCache.asMap().computeIfAbsent(table,
                key -> schemaAccessor.listTableColumns(key.getSchemaName(), key.getTableName()));
    }

    private DBView tryToGetViewColumnsFromCache(@NonNull ConnectionSession connectionSession,
            @NonNull DBSchemaAccessor schemaAccessor, @NonNull TableIdentity table) {
        Cache<TableIdentity, DBView> viewColumnCache = ConnectionSessionUtil.getViewColumnCache(connectionSession);
        return viewColumnCache.asMap().computeIfAbsent(table,
                key -> schemaAccessor.getView(table.getSchemaName(), table.getTableName()));
    }

    public void initSqlType(DialectType dialectType) {
        try {
            BasicResult basicResult = new BasicResult(SqlType.UNKNOWN);
            if (dialectType.isMysql()) {
                basicResult = ParserUtil.parseMysqlType(executeSql, 15000);
            } else if (dialectType.isOracle()) {
                basicResult = ParserUtil.parseOracleType(executeSql, 15000);
            }
            this.generalSqlType = ParserUtil.getGeneralSqlType(basicResult);
            if (Objects.isNull(basicResult.getSqlType()) || SqlType.UNKNOWN == basicResult.getSqlType()) {
                this.sqlType = SqlType.UNKNOWN.name();
            } else {
                this.sqlType = basicResult.getSqlType().name();
                if (Objects.isNull(basicResult.getDbObjectType())) {
                    setDbObjectType("UNKNOWN");
                } else {
                    setDbObjectType(basicResult.getDbObjectType().name());
                }
                setDbObjectNameList(basicResult.getDbObjectNameList());
                if (Objects.nonNull(basicResult.getDbObjectNameList())
                        && basicResult.getDbObjectNameList().size() > 0) {
                    setDbObjectName(basicResult.getDbObjectNameList().get(0));
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to recognize sql type, sql={}", executeSql, e);
        }
    }

    public void setDbObjectName(String name) {
        this.dbObjectNameList = Collections.singletonList(name);
        this.dbObjectName = name;
    }

    public ExecutionTimer getTimer() {
        if (!traceWatch.isClosed()) {
            traceWatch.close();
        }
        return new ExecutionTimer(this.traceWatch);
    }

    private void init(@NonNull JdbcGeneralResult generalResult) {
        this.connectionReset = generalResult.isConnectionReset();
        this.sqlId = generalResult.getSqlTuple().getSqlId();
        this.executeSql = generalResult.getSqlTuple().getExecutedSql();
        this.originSql = generalResult.getSqlTuple().getOriginalSql();
        this.traceWatch = generalResult.getSqlTuple().getSqlWatch();
        this.dbmsOutput = generalResult.getDbmsOutput();
        this.traceId = generalResult.getTraceId();
        this.existWarnings = generalResult.isExistWarnings();
        if (generalResult.getStatus() == SqlExecuteStatus.CANCELED) {
            this.status = SqlExecuteStatus.CANCELED;
            this.track = ErrorCodes.ObExecuteSqlCanceled.getLocalizedMessage(new Object[] {"Pre-sql execution error"});
            if (connectionReset) {
                String errMsg = ErrorCodes.ConnectionReset.getLocalizedMessage(new Object[] {});
                this.track = ErrorCodes.ObExecuteSqlCanceled.getLocalizedMessage(new Object[] {errMsg});
            }
            return;
        }
        try {
            JdbcQueryResult queryResult = generalResult.getQueryResult();
            this.status = SqlExecuteStatus.SUCCESS;
            if (queryResult != null) {
                List<JdbcColumnMetaData> columnMetaDataList = queryResult.getMetaData().getColumns();
                this.columnLabels = new LinkedList<>();
                this.columns = new LinkedList<>();
                this.types = new HashMap<>();
                this.typeNames = new HashMap<>();
                for (JdbcColumnMetaData columnMetaData : columnMetaDataList) {
                    String columnName = columnMetaData.getColumnName();
                    String aliaName = columnMetaData.getColumnLabel();
                    if (StringUtils.isNotBlank(aliaName)) {
                        columnName = aliaName;
                    }
                    columns.add(columnName);
                    columnLabels.add(columnMetaData.getColumnLabel());
                    types.put(columnName, columnMetaData.getColumnType());
                    typeNames.put(columnName, columnMetaData.getColumnTypeName());
                }
                rows = queryResult.getRows();
                this.total = rows.size();
                this.resultSetMetaData = new DBResultSetMetaData();
                this.resultSetMetaData.setFieldMetaDataList(columnMetaDataList);
            }
        } catch (Exception exception) {
            // query error
            this.status = SqlExecuteStatus.FAILED;
            this.track = getTrackMessage(exception);
        }
        try {
            if (this.rows.size() == 0) {
                this.total = generalResult.getAffectRows();
            }
            this.status = SqlExecuteStatus.SUCCESS;
        } catch (Exception exception) {
            // update error
            this.status = SqlExecuteStatus.FAILED;
            this.track = getTrackMessage(exception);
        }
    }

    public static String getTrackMessage(Exception ex) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        if (rootCause instanceof SocketTimeoutException) {
            return ErrorCodes.ObExecuteSqlSocketTimeout.getLocalizedMessage(new Object[] {rootCause.getMessage()});
        }
        if (rootCause instanceof SQLTimeoutException) {
            SQLTimeoutException e = (SQLTimeoutException) ex;
            String message = String.format("ErrorCode = %d, SQLState = %s, Details = %s",
                    e.getErrorCode(), e.getSQLState(), rootCause.getMessage());
            if (OdcConstants.QUERY_EXECUTION_INTERRUPTED.equals(e.getSQLState())) {
                return ErrorCodes.ObExecuteSqlCanceled.getLocalizedMessage(new Object[] {message});
            }
            return ErrorCodes.ObExecuteSqlTimeout.getLocalizedMessage(new Object[] {message});
        }
        if (ex instanceof SQLException) {
            SQLException e = (SQLException) ex;
            return String.format("ErrorCode = %d, SQLState = %s, Details = %s",
                    e.getErrorCode(), e.getSQLState(), rootCause.getMessage());
        }
        return rootCause.getMessage();
    }

    @Override
    public String toString() {
        return "SqlId: " + sqlId + "\nStatus: " + status + "\nOriginSql: "
                + originSql + "\nTotal: " + total + "\nTrack: " + track;
    }

    @Getter
    static class ExecutionTimer {
        private final String name;
        private final long startTimeMillis;
        private final long totalDurationMicroseconds;
        private final List<ExecutionStage> stages = new LinkedList<>();

        public ExecutionTimer(@NonNull TraceWatch traceWatch) {
            List<TraceStage> subStages = traceWatch.getStageList();
            if (CollectionUtils.isNotEmpty(subStages)) {
                for (TraceStage stage : subStages) {
                    stages.add(new ExecutionStage(stage));
                }
            }
            this.name = traceWatch.getId();
            this.startTimeMillis = traceWatch.getStartTimeMillis();
            this.totalDurationMicroseconds = traceWatch.getTotalTime(TimeUnit.MICROSECONDS);
        }
    }

    @Getter
    static class ExecutionStage {
        private final String stageName;
        private final long startTimeMillis;
        private final long totalDurationMicroseconds;
        private final List<ExecutionStage> subStages = new LinkedList<>();

        public ExecutionStage(@NonNull TraceStage traceStage) {
            List<TraceStage> stages = traceStage.getSubStageList();
            if (CollectionUtils.isNotEmpty(stages)) {
                for (TraceStage stage : stages) {
                    subStages.add(new ExecutionStage(stage));
                }
            }
            this.stageName = traceStage.getMessage();
            this.startTimeMillis = traceStage.getStartTime();
            this.totalDurationMicroseconds = traceStage.getTime(TimeUnit.MICROSECONDS);
        }
    }

}
