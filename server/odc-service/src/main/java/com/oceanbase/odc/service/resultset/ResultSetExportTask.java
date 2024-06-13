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
package com.oceanbase.odc.service.resultset;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.MaskValueType;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferJob;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.common.util.FileConvertUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.service.flow.task.model.ResultSetExportResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;
import com.oceanbase.odc.service.session.initializer.ConsoleTimeoutInitializer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2021/11/22 下午3:46
 * @Description: [OBDumper task wrapper]
 */
public class ResultSetExportTask implements Callable<ResultSetExportResult> {
    protected static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private final ResultSetExportTaskParameter parameter;
    private final String fileName;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final File logDir;
    private final File workingDir;
    private final DataTransferConfig transferConfig;
    private final ConnectionSession session;
    private final DataTransferProperties dataTransferProperties;
    private final Long maxDumpSizeBytes;
    @Getter
    private DataTransferJob job;

    public ResultSetExportTask(File workingDir, File logDir, ResultSetExportTaskParameter parameter,
            ConnectionSession session, CloudObjectStorageService cloudObjectStorageService,
            DataTransferProperties dataTransferProperties, Long maxDumpSizeBytes) {
        PreConditions.notBlank(parameter.getFileName(), "req.fileName");
        this.parameter = parameter;
        this.logDir = logDir;
        this.workingDir = workingDir;
        this.fileName = parameter.getFileName();
        this.session = session;
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.dataTransferProperties = dataTransferProperties;
        this.transferConfig = convertParam2TransferConfig(parameter);
        this.maxDumpSizeBytes = maxDumpSizeBytes;
    }

    @Override
    public ResultSetExportResult call() throws Exception {
        try {
            TraceContextHolder.put(DataTransferConstants.LOG_PATH_NAME, logDir.getPath());

            this.job = TaskPluginUtil
                    .getDataTransferExtension(transferConfig.getConnectionInfo().getConnectType().getDialectType())
                    .generate(transferConfig, workingDir, logDir, Collections.emptyList());

            DataTransferTaskResult result = job.call();
            validateSuccessful(result);

            String localResultSetFilePath = getDumpFilePath(result,
                    parameter.getFileFormat() == DataTransferFormat.SQL ? DataTransferFormat.SQL.getExtension()
                            : DataTransferFormat.CSV.getExtension());
            /*
             * 对于空结果集，OBDumper 不生成文件，ODC 需要生成一个空文件以免报错
             */
            File origin = new File(localResultSetFilePath);
            if (!origin.exists()) {
                FileUtils.touch(origin);
            }

            /*
             * OBDumper 不支持 excel 导出，需要先生成 csv, 然后使用工具类转换成 xlsx
             */
            if (DataTransferFormat.EXCEL == parameter.getFileFormat()) {
                String excelFilePath = getDumpFileDirectory() + getFileName(DataTransferFormat.EXCEL.getExtension());
                try {
                    FileConvertUtils.convertCsvToXls(localResultSetFilePath, excelFilePath,
                            parameter.isSaveSql() ? Collections.singletonList(parameter.getSql()) : null);
                } catch (Exception ex) {
                    LOGGER.warn("CSV has been dumped successfully, but converting to Excel failed.");
                    throw ex;
                }
                origin = new File(excelFilePath);
            }

            try {
                String returnVal = handleExportFile(origin);
                LOGGER.info("ResultSetExportTask has been executed successfully");
                return ResultSetExportResult.succeed(returnVal);
            } catch (Exception e) {
                LOGGER.warn("Post processing export file failed.");
                throw e;
            }
        } catch (Exception e) {
            LOGGER.warn("ResultSetExportTask failed.", e);
            throw e;
        }
    }

    private DataTransferConfig convertParam2TransferConfig(ResultSetExportTaskParameter parameter) {
        DataTransferConfig config = new DataTransferConfig();
        config.setSchemaName(parameter.getDatabase());
        config.setTransferType(DataTransferType.EXPORT);
        config.setDataTransferFormat(parameter.getFileFormat());
        config.setTransferData(true);
        config.setTransferDDL(false);
        config.setExportFileMaxSize(-1);
        config.setEncoding(parameter.getFileEncoding());

        String table = StringUtils.isEmpty(parameter.getTableName()) ? "CUSTOM_SQL" : parameter.getTableName();
        config.setExportDbObjects(Collections.singletonList(new DataTransferObject(ObjectType.TABLE, table)));

        CsvConfig csvConfig = new CsvConfig();
        if (parameter.getCsvFormat() != null) {
            csvConfig.setEncoding(parameter.getFileEncoding());
            csvConfig.setBlankToNull(parameter.getCsvFormat().isTransferEmptyString);
            csvConfig.setSkipHeader(!parameter.getCsvFormat().isContainColumnHeader);
            csvConfig.setColumnSeparator(parameter.getCsvFormat().getColumnSeparator());
            csvConfig.setColumnDelimiter(parameter.getCsvFormat().getColumnDelimiter());
            csvConfig.setLineSeparator(parameter.getCsvFormat().getLineSeparator());
        }
        if (parameter.getFileFormat() == DataTransferFormat.EXCEL) {
            csvConfig.setEncoding(parameter.getFileEncoding());
            csvConfig.setColumnSeparator(',');
            csvConfig.setColumnDelimiter('"');
            csvConfig.setLineSeparator("\n");
            csvConfig.setBlankToNull(true);
        }
        config.setCsvConfig(csvConfig);

        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        ConnectionInfo connectionInfo = connectionConfig.toConnectionInfo();
        String initScript = connectionConfig.getSessionInitScript();
        if (StringUtils.isNotEmpty(initScript)) {
            connectionInfo.setSessionInitScripts(SqlCommentProcessor
                    .removeSqlComments(initScript, ";", connectionInfo.getConnectType().getDialectType(), false)
                    .stream().map(OffsetString::getStr).collect(Collectors.toList()));
        }

        connectionInfo.setSchema(parameter.getDatabase());
        config.setConnectionInfo(connectionInfo);

        config.setMaxDumpSizeBytes(maxDumpSizeBytes);

        config.setQuerySql(parameter.getSql());
        config.setFileType(parameter.getFileFormat().name());
        setColumnConfig(config, parameter);
        config.setCursorFetchSize(dataTransferProperties.getCursorFetchSize());
        config.setUsePrepStmts(dataTransferProperties.isUseServerPrepStmts());

        config.setExecutionTimeoutSeconds(parameter.getExecutionTimeoutSeconds());

        return config;
    }

    private void setColumnConfig(DataTransferConfig config, ResultSetExportTaskParameter parameter) {
        List<DBTableColumn> tableColumns = new ArrayList<>();
        config.setColumns(tableColumns);
        HashMap<TableIdentity, Map<String, AbstractDataMasker>> maskConfigMap = new HashMap<>();
        config.setMaskConfig(maskConfigMap);
        List<MaskingAlgorithm> algorithms = parameter.getRowDataMaskingAlgorithms();
        Map<String, Map<String, List<OrdinalColumn>>> catalog2TableColumns = new HashMap<>();
        try {
            SyncJdbcExecutor syncJdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
            syncJdbcExecutor.execute((StatementCallback<?>) stmt -> {
                stmt.setMaxRows(10);
                new ConsoleTimeoutInitializer(parameter.getExecutionTimeoutSeconds() * 1000000L,
                        config.getConnectionInfo().getConnectType().getDialectType()).init(stmt.getConnection());

                stmt.execute(parameter.getSql());
                ResultSetMetaData rsMetaData = stmt.getResultSet().getMetaData();
                if (rsMetaData == null) {
                    throw new UnexpectedException("Query rs metadata failed.");
                }
                int columnCount = rsMetaData.getColumnCount();
                for (int index = 1; index <= columnCount; index++) {
                    String catalogName = rsMetaData.getCatalogName(index);
                    String tableName = rsMetaData.getTableName(index);
                    String columnName = rsMetaData.getColumnName(index);
                    Map<String, List<OrdinalColumn>> table2Columns =
                            catalog2TableColumns.computeIfAbsent(catalogName, k -> new HashMap<>());
                    List<OrdinalColumn> columns = table2Columns.computeIfAbsent(tableName, k -> new ArrayList<>());
                    columns.add(new OrdinalColumn(index - 1, columnName));
                    DBTableColumn column = new DBTableColumn();
                    column.setSchemaName(catalogName);
                    column.setTableName(tableName);
                    column.setName(columnName);
                    tableColumns.add(column);
                }
                return null;
            });
        } catch (Exception e) {
            throw OBException.executeFailed(
                    "Query result metadata failed, please try again, message=" + ExceptionUtils.getRootCauseMessage(e));
        }
        if (!needDataMasking(algorithms)) {
            return;
        }
        DataMaskerFactory maskerFactory = new DataMaskerFactory();
        for (String catalogName : catalog2TableColumns.keySet()) {
            Map<String, List<OrdinalColumn>> tableName2Columns = catalog2TableColumns.get(catalogName);
            for (String tableName : tableName2Columns.keySet()) {
                List<OrdinalColumn> ordinalColumns = tableName2Columns.get(tableName);
                Map<String, AbstractDataMasker> column2Masker = new HashMap<>();
                for (OrdinalColumn column : ordinalColumns) {
                    if (Objects.isNull(algorithms.get(column.getOrdinal()))) {
                        continue;
                    }
                    MaskConfig maskConfig = MaskingAlgorithmUtil
                            .toSingleFieldMaskConfig(algorithms.get(column.getOrdinal()), column.getColumnName());
                    AbstractDataMasker masker =
                            maskerFactory.createDataMasker(MaskValueType.SINGLE_VALUE.name(), maskConfig);
                    column2Masker.put(column.getColumnName(), masker);
                }
                maskConfigMap.put(TableIdentity.of(catalogName, tableName), column2Masker);
            }
        }
    }

    private void validateSuccessful(DataTransferTaskResult result) {
        Verify.verify(CollectionUtils.isEmpty(result.getSchemaObjectsInfo()), "There shouldn't be any schema object");
        Verify.singleton(result.getDataObjectsInfo(), "Exported objects");
        Verify.verify(result.getDataObjectsInfo().get(0).getStatus() == Status.SUCCESS, "Result export task failed!");
    }

    private String getDumpFilePath(DataTransferTaskResult result, String extension) throws Exception {
        List<URL> exportPaths = result.getDataObjectsInfo().get(0).getExportPaths();
        if (CollectionUtils.isEmpty(exportPaths)) {
            return Paths.get(getDumpFileDirectory(), getFileName(extension)).toString();
        }
        return exportPaths.get(0).toURI().getPath();
    }

    private String getFileName(String extension) {
        return parameter.getTableName() + extension;
    }

    private String getDumpFileDirectory() throws IOException {
        File dir = Paths.get(workingDir.getPath(), "data", parameter.getDatabase(), "TABLE").toFile();
        if (!dir.exists()) {
            FileUtils.forceMkdir(dir);
        }
        return dir.getPath();
    }

    private boolean needDataMasking(List<MaskingAlgorithm> algorithms) {
        if (CollectionUtils.isEmpty(algorithms)) {
            return false;
        }
        for (MaskingAlgorithm algorithm : algorithms) {
            if (Objects.nonNull(algorithm)) {
                return true;
            }
        }
        return false;
    }

    private String handleExportFile(File origin) throws Exception {
        try {
            if (cloudObjectStorageService.supported()) {
                try {
                    return cloudObjectStorageService.uploadTemp(fileName, origin);
                } catch (Exception e) {
                    throw new UnexpectedException("upload result set export file to Object Storage failed", e);
                }
            } else {
                File dest = Paths.get(workingDir.getPath(), fileName).toFile();
                if (dest.exists()) {
                    FileUtils.deleteQuietly(dest);
                }
                FileUtils.moveFile(origin, dest);
                return dest.getName();
            }
        } finally {
            FileUtils.deleteQuietly(origin);
        }
    }

    @Data
    @AllArgsConstructor
    private static class OrdinalColumn {
        private int ordinal;
        private String columnName;
    }
}
