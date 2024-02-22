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
package com.oceanbase.odc.service.task.executor.task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.StatementCallback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.CSVUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.initializer.ConsoleTimeoutInitializer;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.server.ObjectStorageHandler;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.dbbrowser.parser.ParserUtil;
import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/3/20
 */

@Slf4j
public class DatabaseChangeTask extends BaseTask<FlowTaskResult> {

    private ConnectionSession connectionSession;
    private DatabaseChangeParameters parameters;
    private InputStream sqlInputStream;
    private SqlStatementIterator sqlIterator;
    private Integer failCount = 0;
    private Integer successCount = 0;
    private long sqlTotalBytes = 0;
    private long sqlReadBytes = 0;
    private String errorRecordsFilePath = null;
    private String fileRootDir;
    private String jsonFileName;
    private String jsonFilePath;
    private File jsonFile;
    private String zipFileId;
    private String zipFileRootPath;
    private String zipFileDownloadUrl;
    private int csvFileIndex = 0;
    private final List<CSVExecuteResult> csvFileMappers = new ArrayList<>();
    private final List<SqlExecuteResult> queryResultSetBuffer = new ArrayList<>();
    private boolean containQuery = false;
    private volatile boolean aborted = false;
    private volatile boolean canceled = false;
    private long taskId;

    private static final Long DEFAULT_DATA_SOURCE_ID = 1L;

    @Override
    protected void doInit(JobContext context) {
        taskId = getJobContext().getJobIdentity().getId();
        log.info("Initiating database change task, taskId={}", taskId);
        parameters = JsonUtils.fromJson(getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DatabaseChangeParameters.class);
        log.info("Load database change task parameters successfully, taskId={}", taskId);
        connectionSession = generateSession();
        String delimiter = Objects.isNull(parameters.getDelimiter()) ? ";" : parameters.getDelimiter();
        ConnectionSessionUtil.getSqlCommentProcessor(connectionSession).setDelimiter(delimiter);
        log.info("Generate connection session successfully, taskId={}", taskId);
        if (StringUtils.isNotEmpty(parameters.getSqlContent())) {
            byte[] sqlBytes = parameters.getSqlContent().getBytes(StandardCharsets.UTF_8);
            sqlInputStream = new ByteArrayInputStream(sqlBytes);
            sqlTotalBytes = sqlBytes.length;
        } else {
            try {
                sqlInputStream = readSqlFilesStream();
            } catch (IOException exception) {
                throw new InternalServerError("Load database change task file failed", exception);
            }
        }
        sqlIterator = SqlUtils.iterator(connectionSession, sqlInputStream, StandardCharsets.UTF_8);
        log.info("Load sql content successfully, taskId={}", taskId);
        try {
            fileRootDir = FileManager.generateDir(FileBucket.ASYNC);
            jsonFileName = StringUtils.uuid();
            jsonFilePath = String.format("%s/%s.json", fileRootDir, jsonFileName);
            jsonFile = new File(jsonFilePath);
            zipFileId = StringUtils.uuid();
            zipFileRootPath = String.format("%s/%s", fileRootDir, zipFileId);
            zipFileDownloadUrl = String.format("/api/v2/flow/flowInstances/%s/tasks/download",
                    Long.parseLong(getJobParameters().get(JobParametersKeyConstants.FLOW_INSTANCE_ID)));
        } catch (Exception exception) {
            throw new InternalServerError("Create database change task file dir failed", exception);
        }
    }

    @Override
    protected void doStart(JobContext context) {
        try {
            int index = 0;
            while (sqlIterator.hasNext()) {
                if (canceled) {
                    log.info("Accept cancel task request, taskId={}", taskId);
                    break;
                }
                if (aborted) {
                    log.info("Task is aborted, remaining sql will be skipped, taskId={}", taskId);
                    break;
                }
                String sql = sqlIterator.next().getStr();
                sqlReadBytes = sqlIterator.iteratedBytes();
                index++;
                log.info("Database change sql: {}", sql);
                try {
                    List<SqlTuple> sqlTuples = Collections.singletonList(SqlTuple.newTuple(sql));
                    OdcStatementCallBack statementCallback =
                            new OdcStatementCallBack(sqlTuples, connectionSession, true, parameters.getQueryLimit());
                    statementCallback.setMaxCachedLines(0);
                    statementCallback.setMaxCachedSize(0);
                    statementCallback.setDbmsoutputMaxRows(0);
                    JdbcOperations executor =
                            connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
                    long timeoutUs = TimeUnit.MILLISECONDS.toMicros(parameters.getTimeoutMillis());
                    if (timeoutUs < 0) {
                        throw new IllegalArgumentException(
                                "Timeout settings is too large, " + parameters.getTimeoutMillis());
                    }
                    ConnectionInitializer initializer = new ConsoleTimeoutInitializer(timeoutUs);
                    executor.execute((ConnectionCallback<Void>) con -> {
                        initializer.init(con);
                        return null;
                    });
                    List<JdbcGeneralResult> results =
                            executor.execute((StatementCallback<List<JdbcGeneralResult>>) stmt -> {
                                stmt.setQueryTimeout(
                                        (int) TimeUnit.MILLISECONDS.toSeconds(parameters.getTimeoutMillis()));
                                return statementCallback.doInStatement(stmt);
                            });
                    Verify.notEmpty(results, "resultList");
                    GeneralSqlType sqlType = parseSqlType(sql);
                    for (JdbcGeneralResult result : results) {
                        SqlExecuteResult executeResult = new SqlExecuteResult(result);
                        if (GeneralSqlType.DQL == sqlType) {
                            containQuery = true;
                        }
                        queryResultSetBuffer.add(executeResult);
                    }
                    appendResultToJsonFile(queryResultSetBuffer, index == 1, !sqlIterator.hasNext());
                    writeCsvFiles(queryResultSetBuffer);
                    queryResultSetBuffer.clear();
                    boolean success = true;
                    for (JdbcGeneralResult result : results) {
                        SqlExecuteResult executeResult = new SqlExecuteResult(result);
                        if (executeResult.getStatus() != SqlExecuteStatus.SUCCESS) {
                            failCount++;
                            log.warn("Error occurs when executing sql: {}, error message: {}", sql,
                                    executeResult.getTrack());
                            // only record info of failed sqls
                            addErrorRecordsToFile(index, sql, executeResult.getTrack());
                            success = false;
                            break;
                        }
                    }
                    if (success) {
                        successCount++;
                    } else {
                        if (TaskErrorStrategy.ABORT.equals(parameters.getErrorStrategy())) {
                            aborted = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    failCount++;
                    log.warn("Error occurs when executing sql={} :", sql, e);
                    // only record info of failed sql
                    addErrorRecordsToFile(index, sql, e.getMessage());
                    if (TaskErrorStrategy.ABORT.equals(parameters.getErrorStrategy())) {
                        aborted = true;
                        break;
                    }
                }
            }
            writeZipFile();
            log.info("Database change task end up running, task id: {}", taskId);
        } finally {
            tryExpireConnectionSession();
            tryCloseInputStream();
        }
    }

    @Override
    protected void doStop() {
        tryExpireConnectionSession();
        tryCloseInputStream();
        canceled = true;
    }

    @Override
    protected void onFail(Throwable e) {
        tryExpireConnectionSession();
        tryCloseInputStream();
    }

    @Override
    public double getProgress() {
        if (sqlTotalBytes == 0) {
            // do nothing and done
            return 100.0D;
        }
        double progress = sqlReadBytes * 100.0D / sqlTotalBytes;
        return Math.min(progress, 100.0D);
    }

    @Override
    public FlowTaskResult getTaskResult() {
        DatabaseChangeResult taskResult = new DatabaseChangeResult();
        taskResult.setFailCount(failCount);
        taskResult.setSuccessCount(successCount);
        taskResult.setZipFileDownloadUrl(zipFileDownloadUrl);
        taskResult.setZipFileId(zipFileId);
        taskResult.setJsonFileName(jsonFileName);
        taskResult.setContainQuery(containQuery);
        taskResult.setErrorRecordsFilePath(errorRecordsFilePath);
        return taskResult;
    }

    private ConnectionSession generateSession() {
        ConnectionConfig connectionConfig = JsonUtils
                .fromJson(getJobParameters().get(JobParametersKeyConstants.CONNECTION_CONFIG), ConnectionConfig.class);
        connectionConfig.setId(DEFAULT_DATA_SOURCE_ID);
        connectionConfig.setDefaultSchema(getJobParameters().get(JobParametersKeyConstants.CURRENT_SCHEMA));
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        sessionFactory.setSessionTimeoutMillis(parameters.getTimeoutMillis());
        ConnectionSession connectionSession = sessionFactory.generateSession();
        if (connectionSession.getDialectType().isOracle()) {
            ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession,
                    getJobParameters().get(JobParametersKeyConstants.SESSION_TIME_ZONE));
        }
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true, true);
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);
        ConnectionSessionUtil.setColumnAccessor(connectionSession, new DatasourceColumnAccessor(connectionSession));
        return connectionSession;
    }

    private InputStream readSqlFilesStream() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        List<ObjectMetadata> objectMetadataList =
                JsonUtils.fromJson(getJobParameters().get(JobParametersKeyConstants.OBJECT_METADATA),
                        new TypeReference<List<ObjectMetadata>>() {});
        for (ObjectMetadata objectMetadata : objectMetadataList) {
            StorageObject object =
                    new ObjectStorageHandler(getCloudObjectStorageService(), JobUtils.getExecutorDataPath())
                            .loadObject(objectMetadata);
            InputStream current = object.getContent();
            sqlTotalBytes += object.getMetadata().getTotalLength();
            // remove UTF-8 BOM if exists
            current.mark(3);
            byte[] byteSql = new byte[3];
            if (current.read(byteSql) >= 3 && byteSql[0] == (byte) 0xef && byteSql[1] == (byte) 0xbb
                    && byteSql[2] == (byte) 0xbf) {
                current.reset();
                current.skip(3);
                sqlTotalBytes -= 3;
            } else {
                current.reset();
            }
            inputStream = new SequenceInputStream(inputStream, current);
        }
        return inputStream;
    }

    private void appendResultToJsonFile(List<SqlExecuteResult> results, boolean start, boolean end) {
        try {
            if (start) {
                FileUtils.writeStringToFile(jsonFile, "[", StandardCharsets.UTF_8, true);
            }
            if (!results.isEmpty()) {
                if (!start) {
                    FileUtils.writeStringToFile(jsonFile, ",", StandardCharsets.UTF_8, true);
                }
                String jsonString = JsonUtils.toJson(results);
                FileUtils.writeStringToFile(jsonFile, jsonString.substring(1, jsonString.length() - 1),
                        StandardCharsets.UTF_8, true);
            }
            if (end) {
                FileUtils.writeStringToFile(jsonFile, "]", StandardCharsets.UTF_8, true);
            }
            log.info("Database change task result set was saved as JSON file successfully, file name={}", jsonFilePath);
        } catch (IOException e) {
            log.warn("Build JSON file failed, errorMessage={}", e.getMessage());
            throw new UnexpectedException("build JSON file failed");
        }
    }

    private void writeCsvFiles(List<SqlExecuteResult> results) {
        try {
            for (SqlExecuteResult result : results) {
                csvFileIndex++;
                if (Objects.isNull(result.getRows()) || result.getRows().isEmpty()) {
                    continue;
                }
                String csv = CSVUtils.buildCSVFormatData(result.getColumns(), result.getRows());
                String filePath = String.format("%s/%s.csv", zipFileRootPath, csvFileIndex);
                FileUtils.writeStringToFile(new File(filePath), csv, StandardCharsets.UTF_8, true);
                String executeSql = result.getExecuteSql();
                String fileName = String.format("%s.csv", csvFileIndex);
                CSVExecuteResult csvExecuteResult = new CSVExecuteResult(csvFileIndex, executeSql, fileName);
                csvFileMappers.add(csvExecuteResult);
            }
        } catch (IOException ex) {
            throw new UnexpectedException("Write csv file failed");
        }
    }

    private void addErrorRecordsToFile(int index, String sql, String errorMsg) {
        if (StringUtils.isBlank(errorRecordsFilePath)) {
            errorRecordsFilePath = fileRootDir + File.separator + StringUtils.uuid() + ".txt";
        }
        try (FileWriter fw = new FileWriter(errorRecordsFilePath, true)) {
            String modifiedErrorMsg = generateErrorRecord(index, sql, errorMsg);
            fw.append(modifiedErrorMsg);
        } catch (IOException ex) {
            log.warn("Generate error record failed, sql index={}, sql={}, errorMsg={}", index, sql, errorMsg);
        }
    }

    private void writeZipFile() {
        try {
            String jsonString = JsonUtils.prettyToJson(csvFileMappers);
            FileUtils.writeStringToFile(new File(String.format("%s/csv_execute_result.json", zipFileRootPath)),
                    jsonString, StandardCharsets.UTF_8, true);
            OdcFileUtil.zip(String.format(zipFileRootPath), String.format("%s.zip", zipFileRootPath));
            log.info("Database change task result set was saved as local zip file, file name={}", zipFileId);
            // Public cloud scenario, need to upload files to OSS
            CloudObjectStorageService cloudObjectStorageService = getCloudObjectStorageService();
            if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
                File tempZipFile = new File(String.format("%s.zip", zipFileRootPath));
                try {
                    String objectName = cloudObjectStorageService.uploadTemp(zipFileId + ".zip", tempZipFile);
                    zipFileDownloadUrl = cloudObjectStorageService.getBucketName() + "/" + objectName;
                    log.info("Upload database change task result set zip file to OSS, file name={}", zipFileId);
                } catch (Exception exception) {
                    log.warn("Upload database change task result set zip file to OSS failed, file name={}", zipFileId);
                    throw new RuntimeException(String.format(
                            "Upload database change task result set zip file to OSS failed, file name: %s", zipFileId),
                            exception.getCause());
                } finally {
                    OdcFileUtil.deleteFiles(tempZipFile);
                }
            }
            FileUtils.deleteDirectory(new File(zipFileRootPath));
        } catch (IOException ex) {
            throw new UnexpectedException("Build zip file failed");
        }
    }

    private String generateErrorRecord(int index, String sql, String errorMsg) {
        StringBuilder stringBuilder = new StringBuilder();
        String localizedMsg = ErrorCodes.TaskSqlExecuteFailed.getEnglishMessage(new Object[] {index});
        stringBuilder.append(localizedMsg).append(": ").append(sql).append(' ').append(errorMsg);
        return StringUtils.singleLine(stringBuilder.toString()) + "\n";
    }

    private GeneralSqlType parseSqlType(String sql) {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories
                .getAstFactory(connectionSession.getDialectType(), 0);
        if (factory == null) {
            return GeneralSqlType.OTHER;
        }
        try {
            return ParserUtil.getGeneralSqlType(factory.buildAst(sql).getParseResult());
        } catch (Exception e) {
            return GeneralSqlType.OTHER;
        }
    }

    private void tryExpireConnectionSession() {
        if (connectionSession != null && !connectionSession.isExpired()) {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }
    }

    private void tryCloseInputStream() {
        if (Objects.nonNull(sqlInputStream)) {
            try {
                sqlInputStream.close();
            } catch (Exception e) {
                // eat exception
            }
        }
    }

    /**
     * Record CSVFile name with its corresponding sql
     */
    @Data
    static class CSVExecuteResult {
        private int sequence;
        private String sql;
        private String fileName;

        CSVExecuteResult(int sequence, String sql, String fileName) {
            this.sequence = sequence;
            this.sql = sql;
            this.fileName = fileName;
        }
    }

}
