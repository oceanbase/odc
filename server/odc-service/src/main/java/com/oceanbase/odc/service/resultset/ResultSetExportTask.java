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
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
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
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.FileConvertUtils;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.datasecurity.DataMaskingFunction;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.flow.task.OssTaskReferManager;
import com.oceanbase.odc.service.flow.task.model.ResultSetExportResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter.CSVFormat;
import com.oceanbase.tools.loaddump.client.DumpClient;
import com.oceanbase.tools.loaddump.client.DumpClient.Builder;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.TaskDetail;
import com.oceanbase.tools.loaddump.context.TaskContext;
import com.oceanbase.tools.loaddump.function.context.ControlContext;
import com.oceanbase.tools.loaddump.function.context.ControlDescription;
import com.oceanbase.tools.loaddump.manager.ControlManager;
import com.oceanbase.tools.loaddump.parser.record.csv.CsvFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2021/11/22 下午3:46
 * @Description: [OBDumper task wrapper]
 */
public class ResultSetExportTask implements Callable<ResultSetExportResult> {
    protected static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private DumpParameter dumpParameter;
    private final ResultSetExportTaskParameter parameter;
    private final String taskId;
    private final String fileName;
    private final CloudObjectStorageService cloudObjectStorageService;
    @Getter
    private TaskContext taskContext;

    public ResultSetExportTask(String taskId, ResultSetExportTaskParameter parameter,
            ConnectionSession session, CloudObjectStorageService cloudObjectStorageService) {
        PreConditions.notBlank(parameter.getFileName(), "req.fileName");
        this.taskId = taskId;
        this.fileName = parameter.getFileName();
        this.parameter = parameter;
        this.cloudObjectStorageService = cloudObjectStorageService;
        initDumpParameter(session);
    }

    private void initDumpParameter(ConnectionSession session) {
        this.dumpParameter = new DumpParameter();
        initBaseParameter();
        initSessionParameter(session);
        setMaskConfig(session);
    }

    private void initCSVParameter(ResultSetExportTaskParameter parameter) {
        if (Objects.nonNull(parameter.getCsvFormat())) {
            CSVFormat csvFormat = parameter.getCsvFormat();
            this.dumpParameter.setIgnoreEmptyLine(true);
            /**
             * column separator
             */
            this.dumpParameter.setColumnSeparator(CsvFormat.DEFAULT.toChar(csvFormat.getColumnSeparator()));
            /**
             * new line char
             */
            String lineSeparator = csvFormat.getLineSeparator();
            String realLineSeparator = "";
            int length = lineSeparator.length();
            boolean transferFlag = false;
            for (int i = 0; i < length; i++) {
                char item = lineSeparator.charAt(i);
                if (item == '\\') {
                    transferFlag = true;
                    continue;
                }
                if (transferFlag) {
                    if (item == 'n') {
                        realLineSeparator += '\n';
                    } else if (item == 'r') {
                        realLineSeparator += '\r';
                    }
                    transferFlag = false;
                } else {
                    realLineSeparator += item;
                }
            }
            this.dumpParameter.setLineSeparator(realLineSeparator);
            /**
             * if skip csv header
             */
            this.dumpParameter.setSkipHeader(!csvFormat.isContainColumnHeader());
            /**
             * column delimiter
             */
            this.dumpParameter.setColumnDelimiter(CsvFormat.DEFAULT.toChar(csvFormat.getColumnDelimiter()));
            if (csvFormat.isTransferEmptyString()) {
                /**
                 * if you are here, you should convert null value to "null". Otherwise, it will be converted to "\N"
                 */
                this.dumpParameter.setNullString("null");
            }
            this.dumpParameter.setEmptyString("");
        }
    }

    private void initBaseParameter() {
        this.dumpParameter.setLogPath(TraceContextHolder.get("task.workspace"));
        this.dumpParameter.setFilePath(FileManager.generateDirPath(FileBucket.RESULT_SET, taskId));
        this.dumpParameter.setQuerySql(parameter.getSql());
        this.dumpParameter.setMaxRows(parameter.getMaxRows());
        this.dumpParameter.setFileEncoding(parameter.getFileEncoding().getAlias());
        this.dumpParameter.setSchemaless(true);
        this.dumpParameter.setSkipCheckDir(true);
        this.dumpParameter.setRetainEmptyFiles(true);
        this.dumpParameter.setBlockSize(-1);
        this.dumpParameter.setSnapshot(false);
        Set<String> whiteList = new HashSet<>();
        whiteList.add(Objects.isNull(parameter.getTableName()) ? "CUSTOM_SQL" : parameter.getTableName());
        dumpParameter.getWhiteListMap().put(ObjectType.TABLE, whiteList);

        if (DataTransferFormat.SQL == parameter.getFileFormat()) {
            this.dumpParameter.setDataFormat(DataFormat.SQL);
            this.dumpParameter.setFileSuffix(".sql");
            this.dumpParameter.setSkipHeader(true);
        } else if (DataTransferFormat.CSV == parameter.getFileFormat()) {
            this.dumpParameter.setDataFormat(DataFormat.CSV);
            this.dumpParameter.setFileSuffix(".csv");
            initCSVParameter(parameter);
        } else if (DataTransferFormat.EXCEL == parameter.getFileFormat()) {
            this.dumpParameter.setDataFormat(DataFormat.CSV);
            this.dumpParameter.setFileSuffix(".csv");
            intExcelParameter(parameter);
        } else {
            throw new UnsupportedException(parameter.getFileFormat() + " not supported");
        }
    }

    private void intExcelParameter(ResultSetExportTaskParameter req) {
        this.initCSVParameter(req);
        this.dumpParameter.setEscapeCharacter('\"');
        this.dumpParameter.setColumnSeparator(',');
        this.dumpParameter.setColumnDelimiter('\"');
        this.dumpParameter.setLineSeparator("\n");
        this.dumpParameter.setIgnoreEmptyLine(false);
        this.dumpParameter.setNullString("null");
    }

    private void initSessionParameter(ConnectionSession session) {
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        this.dumpParameter.setHost(connectionConfig.getHost());
        this.dumpParameter.setPort(connectionConfig.getPort());
        this.dumpParameter.setPassword(connectionConfig.getPassword());
        this.dumpParameter.setCluster(connectionConfig.getClusterName());
        this.dumpParameter.setTenant(connectionConfig.getTenantName());
        String database =
                MoreObjects.firstNonNull(parameter.getDatabase(), ConnectionSessionUtil.getCurrentSchema(session));
        if (DialectType.OB_ORACLE == connectionConfig.getDialectType()) {
            this.dumpParameter.setUser(StringUtils.quoteOracleIdentifier(ConnectionSessionUtil
                    .getUserOrSchemaString(connectionConfig.getUsername(), connectionConfig.getDialectType())));
            this.dumpParameter.setDatabaseName(database);
            this.dumpParameter.setConnectDatabaseName(StringUtils.quoteOracleIdentifier(database));
        } else {
            this.dumpParameter.setUser(ConnectionSessionUtil.getUserOrSchemaString(connectionConfig.getUsername(),
                    connectionConfig.getDialectType()));
            this.dumpParameter.setDatabaseName(database);
            this.dumpParameter.setConnectDatabaseName(database);
        }
        if (StringUtils.isNotBlank(connectionConfig.getSysTenantUsername())) {
            this.dumpParameter.setSysUser(connectionConfig.getSysTenantUsername());
            this.dumpParameter.setSysPassword(connectionConfig.getSysTenantPassword());
        } else {
            if (connectionConfig.getType().isCloud()) {
                LOGGER.info("Sys user does not exist, use cloud mode");
                this.dumpParameter.setPubCloud(true);
            } else {
                LOGGER.info("Sys user does not exist, use no sys mode");
                this.dumpParameter.setNoSys(true);
            }
        }
        OBTenantEndpoint endpoint = connectionConfig.getEndpoint();
        if (Objects.nonNull(endpoint)) {
            if (StringUtils.isNotBlank(endpoint.getProxyHost()) && Objects.nonNull(endpoint.getProxyPort())) {
                this.dumpParameter.setSocksProxyHost(endpoint.getProxyHost());
                this.dumpParameter.setSocksProxyPort(endpoint.getProxyPort().toString());
            }
        }
    }

    private void setMaskConfig(ConnectionSession session) {
        List<MaskingAlgorithm> algorithms = parameter.getRowDataMaskingAlgorithms();
        if (!needDataMasking(algorithms)) {
            return;
        }
        Map<String, Map<String, List<OrdinalColumn>>> catalog2TableColumns = new HashMap<>();
        try {
            SyncJdbcExecutor syncJdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
            ResultSetMetaData rsMetaData =
                    syncJdbcExecutor.query(parameter.getSql(), pss -> pss.setMaxRows(10), ResultSet::getMetaData);
            if (rsMetaData == null) {
                return;
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
            }
        } catch (Exception e) {
            throw OBException.executeFailed(
                    "Query result metadata failed, please try again, message=" + ExceptionUtils.getRootCauseMessage(e));
        }
        ControlManager controlManager = ControlManager.newInstance();
        DataMaskerFactory maskerFactory = new DataMaskerFactory();
        for (String catalogName : catalog2TableColumns.keySet()) {
            Map<String, List<OrdinalColumn>> tableName2Columns = catalog2TableColumns.get(catalogName);
            for (String tableName : tableName2Columns.keySet()) {
                List<OrdinalColumn> ordinalColumns = tableName2Columns.get(tableName);
                ControlContext context = new ControlContext();
                for (OrdinalColumn column : ordinalColumns) {
                    if (Objects.isNull(algorithms.get(column.getOrdinal()))) {
                        continue;
                    }
                    ControlDescription description = new ControlDescription(column.getColumnName());
                    MaskConfig maskConfig = MaskingAlgorithmUtil
                            .toSingleFieldMaskConfig(algorithms.get(column.getOrdinal()), column.getColumnName());
                    AbstractDataMasker masker =
                            maskerFactory.createDataMasker(MaskValueType.SINGLE_VALUE.name(), maskConfig);
                    DataMaskingFunction function = new DataMaskingFunction(masker);
                    description.add(function);
                    context.add(description);
                }
                controlManager.register(catalogName, tableName, context);
            }
        }
        dumpParameter.setControlManager(controlManager);
        dumpParameter.setUseRuntimeTableName(true);
    }

    @Override
    public ResultSetExportResult call() throws Exception {
        try {
            DumpClient dumpClient = new Builder(this.dumpParameter).build();
            taskContext = dumpClient.dumpRecord();
        } catch (Exception e) {
            LOGGER.warn("ResultSetExportTask has been finished with some unexpected error when preparing.");
            throw e;
        }
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (taskContext.isAllTasksFinished()) {
                    if (taskContext.getFailureTaskDetails().size() != 0) {
                        String errorMsg = "some errors happened when dumping result set: ";
                        Collection<TaskDetail> failedTasks = taskContext.getFailureTaskDetails();
                        if (CollectionUtils.isNotEmpty(failedTasks)) {
                            errorMsg += failedTasks.stream()
                                    .map(i -> i.getSchemaTable() + ": " + i.getError())
                                    .collect(Collectors.joining("\n"));
                        }
                        throw new IllegalStateException(errorMsg);
                    }

                    String localResultSetFilePath = getDumpFilePath(this.dumpParameter.getFileSuffix());

                    /**
                     * 对于空结果集，OBDumper 不生成文件，ODC 需要生成一个空文件以免报错
                     */
                    File origin = new File(localResultSetFilePath);
                    if (!origin.exists()) {
                        FileUtils.touch(origin);
                    }

                    /**
                     * OBDumper 不支持 excel 导出，需要先生成 csv, 然后使用工具类转换成 xlsx
                     */
                    if (DataTransferFormat.EXCEL == parameter.getFileFormat()) {
                        String excelFilePath = getDumpFilePath(DataTransferFormat.EXCEL.getExtension());
                        try {
                            FileConvertUtils.convertCsvToXls(localResultSetFilePath, excelFilePath,
                                    parameter.isSaveSql() ? Arrays.asList(parameter.getSql()) : null);
                        } catch (Exception ex) {
                            LOGGER.warn("CSV has been dumped successfully, but converting to Excel failed.");
                            throw ex;
                        }
                        origin = new File(excelFilePath);
                    }

                    try {
                        handleExportFile(origin);
                    } catch (Exception e) {
                        LOGGER.warn("Post processing export file failed.");
                        throw e;
                    }
                    LOGGER.info("ResultSetExportTask has been executed successfully");
                    break;
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                throw new InterruptedException("ResultSetExportTask has been interrupted by force");
            }
        } finally {
            shutdownContext(taskContext);
        }
        return ResultSetExportResult.succeed(fileName);
    }

    private void shutdownContext(@NonNull TaskContext context) {
        try {
            context.shutdown();
            LOGGER.info("shutdown task context finished");
        } catch (Exception e) {
            try {
                context.shutdownNow();
                LOGGER.info("shutdown task context immediately finished");
            } catch (Exception ex) {
                LOGGER.warn("shutdown task context immediately failed, {}", ex.getMessage());
            }
        } finally {
            LOGGER.info(context.getProgress().toString());
            LOGGER.info(context.getSummary().toHumanReadableFormat());
        }
    }


    private String getDumpFilePath(String extension) {
        return getDumpFileDirectory() + getFileName(extension);
    }

    private String getFileName(String extension) {
        return parameter.getTableName() + ".0.0" + extension;
    }

    private String getDumpFileDirectory() {
        return this.dumpParameter.getFilePath() + "/data/" + dumpParameter.getDatabaseName() + "/TABLE/";
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

    @Data
    @AllArgsConstructor
    private static class OrdinalColumn {
        private int ordinal;
        private String columnName;
    }

    private void handleExportFile(File origin) throws Exception {
        try {
            if (cloudObjectStorageService.supported()) {
                try {
                    String objectName = cloudObjectStorageService
                            .uploadTemp(CloudObjectStorageConstants.ODC_SERVER_PREFIX, origin);
                    ((OssTaskReferManager) SpringContextUtil.getBean("ossTaskReferManager")).put(fileName, objectName);
                } catch (Exception exception) {
                    throw new UnexpectedException(String
                            .format("upload result set export file to Object Storage failed, file name: %s", taskId));
                }
            } else {
                File dest = Paths.get(dumpParameter.getFilePath(), fileName).toFile();
                if (dest.exists()) {
                    FileUtils.deleteQuietly(dest);
                }
                FileUtils.moveFile(origin, dest);
            }
        } finally {
            FileUtils.deleteQuietly(origin);
        }
    }

}
