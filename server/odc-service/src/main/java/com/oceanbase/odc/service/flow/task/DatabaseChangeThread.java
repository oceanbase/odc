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
package com.oceanbase.odc.service.flow.task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TaskContextHolder;
import com.oceanbase.odc.common.util.CSVUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datamasking.algorithm.Algorithm;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
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
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.DataMaskingUtil;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.task.model.SizeAwareInputStream;
import com.oceanbase.odc.service.flow.task.util.DatabaseChangeFileReader;
import com.oceanbase.odc.service.flow.task.util.TaskDownloadUrlsProvider;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.initializer.ConsoleTimeoutInitializer;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.dbbrowser.parser.ParserUtil;
import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/3/20
 */

@Slf4j
public class DatabaseChangeThread extends Thread {

    private final ConnectionSession connectionSession;
    private final DatabaseChangeParameters parameters;
    private InputStream sqlInputStream;
    private SqlStatementIterator sqlIterator;
    private String errorRecordsFilePath = null;
    private int failCount = 0;
    private int successCount = 0;
    private long sqlTotalBytes = 0;
    private long sqlReadBytes = 0;
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
    @Getter
    private Long startTimestamp = null;
    @Getter
    private boolean abort = false;
    private boolean isContainQuery = false;
    @Getter
    private volatile Boolean stop = false;
    @Setter
    @Getter
    protected long userId;
    @Getter
    @Setter
    protected long taskId;
    @Getter
    @Setter
    protected long flowInstanceId;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final ObjectStorageFacade objectStorageFacade;
    private final DataMaskingService maskingService;

    public DatabaseChangeThread(ConnectionSession connectionSession, DatabaseChangeParameters parameters,
            CloudObjectStorageService cloudObjectStorageService, ObjectStorageFacade objectStorageFacade,
            DataMaskingService maskingService) {
        this.connectionSession = connectionSession;
        this.parameters = parameters;
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.objectStorageFacade = objectStorageFacade;
        this.maskingService = maskingService;
    }

    @Override
    public void run() {
        TaskContextHolder.trace(userId, taskId);
        log.info("Database change task start to run, taskId={}", this.getTaskId());
        startTimestamp = System.currentTimeMillis();
        try {
            init(userId);
            int index = 0;
            while (sqlIterator.hasNext()) {
                String sql = sqlIterator.next().getStr();
                sqlReadBytes = sqlIterator.iteratedBytes();
                index++;
                log.info("Async sql: {}", sql);
                if (stop) {
                    break;
                }
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
                    ConnectionInitializer initializer =
                            new ConsoleTimeoutInitializer(timeoutUs, connectionSession.getDialectType());
                    executor.execute((ConnectionCallback<Void>) con -> {
                        initializer.init(con);
                        return null;
                    });
                    RetryResult result = new RetryResult();
                    retryStatement(result, executor, statementCallback, sql);
                    appendResultToJsonFile(queryResultSetBuffer, index == 1, !sqlIterator.hasNext());
                    writeCsvFiles(queryResultSetBuffer);
                    queryResultSetBuffer.clear();

                    if (result.success) {
                        successCount++;
                    } else {
                        failCount++;
                        addErrorRecordsToFile(index, sql, result.track);
                        if (TaskErrorStrategy.ABORT.equals(parameters.getErrorStrategy())) {
                            abort = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    failCount++;
                    log.warn("Error occurs when executing sql={} :", sql, e);
                    // only record info of failed sql
                    addErrorRecordsToFile(index, sql, e.getMessage());
                    if (TaskErrorStrategy.ABORT.equals(parameters.getErrorStrategy())) {
                        abort = true;
                        break;
                    }
                }
            }
            writeZipFile();
            log.info("Async task end up running, task id: {}", this.getTaskId());
        } finally {
            TaskContextHolder.clear();
            connectionSession.expire();
            if (Objects.nonNull(sqlInputStream)) {
                try {
                    sqlInputStream.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private void retryStatement(RetryResult retryResult, JdbcOperations executor,
            StatementCallback<List<JdbcGeneralResult>> statementCallback, String sql) {
        boolean success = true;
        GeneralSqlType sqlType = parseSqlType(sql);
        for (int retryCount = 0; retryCount <= parameters.getRetryTimes() && !stop; retryCount++) {
            success = true;
            try {
                queryResultSetBuffer.clear();
                List<JdbcGeneralResult> results =
                        executor.execute((StatementCallback<List<JdbcGeneralResult>>) stmt -> {
                            stmt.setQueryTimeout(
                                    (int) TimeUnit.MILLISECONDS.toSeconds(parameters.getTimeoutMillis()));
                            return statementCallback.doInStatement(stmt);
                        });
                Verify.notEmpty(results, "resultList");
                for (JdbcGeneralResult result : results) {
                    SqlExecuteResult executeResult = new SqlExecuteResult(result);
                    if (GeneralSqlType.DQL == sqlType) {
                        this.isContainQuery = true;
                        if (maskingService.isMaskingEnabled()) {
                            try {
                                dynamicDataMasking(executeResult);
                            } catch (Exception e) {
                                // Eat exception and skip data masking
                                log.warn("Failed to mask query result set in database change task, sql={}",
                                        executeResult.getExecuteSql(), e);
                            }
                        }
                    }
                    queryResultSetBuffer.add(executeResult);

                    if (executeResult.getStatus() != SqlExecuteStatus.SUCCESS) {
                        log.warn("Error occurs when executing sql: {}, error message: {}", sql,
                                executeResult.getTrack());
                        success = false;
                        retryResult.track = executeResult.getTrack();
                    }
                }
            } catch (Exception e) {
                if (retryCount == parameters.getRetryTimes()) {
                    throw e;
                }
                success = false;
                log.warn("Error occurs when executing sql: {}, error message: {}", sql, e.getMessage());
            }
            if (success || retryCount == parameters.getRetryTimes()) {
                break;
            } else {
                log.warn(String.format("Will retry for the %sth time in %s seconds...", retryCount + 1,
                        parameters.getRetryIntervalMillis() / 1000));
                try {
                    Thread.sleep(parameters.getRetryIntervalMillis());
                } catch (InterruptedException e) {
                    log.warn("Database change task is interrupted, task will exit", e);
                    stop = true;
                    break;
                }
            }
        }
        retryResult.success = success;
    }

    private void init(Long userId) {
        log.info("Start read sql content, taskId={}", this.getTaskId());
        List<String> objectIds = parameters.getSqlObjectIds();
        if (StringUtils.isNotEmpty(parameters.getSqlContent())) {
            byte[] sqlBytes = parameters.getSqlContent().getBytes(StandardCharsets.UTF_8);
            sqlInputStream = new ByteArrayInputStream(sqlBytes);
            sqlTotalBytes = sqlBytes.length;
        } else {
            try {
                SizeAwareInputStream sizeAwareInputStream =
                        DatabaseChangeFileReader.readSqlFilesStream(objectStorageFacade,
                                "async".concat(File.separator).concat(String.valueOf(userId)), objectIds, null);
                sqlInputStream = sizeAwareInputStream.getInputStream();
                sqlTotalBytes = sizeAwareInputStream.getTotalBytes();
            } catch (IOException exception) {
                throw new InternalServerError("load database change task file failed", exception);
            }
        }
        String delimiter = Objects.isNull(parameters.getDelimiter()) ? ";" : parameters.getDelimiter();
        ConnectionSessionUtil.getSqlCommentProcessor(connectionSession).setDelimiter(delimiter);
        this.sqlIterator = SqlUtils.iterator(connectionSession, sqlInputStream, StandardCharsets.UTF_8);
        log.info("Read sql content successfully, taskId={}", this.getTaskId());
        try {
            fileRootDir = FileManager.generateDir(FileBucket.ASYNC);
            jsonFileName = StringUtils.uuid();
            jsonFilePath = String.format("%s/%s.json", fileRootDir, jsonFileName);
            jsonFile = new File(jsonFilePath);
            zipFileId = StringUtils.uuid();
            zipFileRootPath = String.format("%s/%s", fileRootDir, zipFileId);
            zipFileDownloadUrl = String.format("/api/v2/flow/flowInstances/%s/tasks/download", flowInstanceId);
        } catch (Exception exception) {
            throw new InternalServerError("create database change task file dir failed", exception);
        }
    }

    private void addErrorRecordsToFile(int index, String sql, String errorMsg) {
        if (StringUtils.isBlank(this.errorRecordsFilePath)) {
            this.errorRecordsFilePath = fileRootDir + File.separator + StringUtils.uuid() + ".txt";
        }
        try (FileWriter fw = new FileWriter(this.errorRecordsFilePath, true)) {
            String modifiedErrorMsg = generateErrorRecord(index, sql, errorMsg);
            fw.append(modifiedErrorMsg);
        } catch (IOException ex) {
            log.warn("generate error record failed, sql index={}, sql={}, errorMsg={}", index, sql, errorMsg);
        }
    }

    public void stopTaskAndKillQuery(DBSessionManageFacade sessionManageFacade) {
        this.stop = true;
        log.info("Try to kill current query");
        try {
            sessionManageFacade.killCurrentQuery(connectionSession);
            log.info("Kill current query success");
        } catch (Exception e) {
            log.warn("Kill current query failed", e);
        }
    }

    public DatabaseChangeResult getResult() {
        DatabaseChangeResult taskResult = new DatabaseChangeResult();
        taskResult.setFailCount(failCount);
        taskResult.setSuccessCount(successCount);
        taskResult.setZipFileDownloadUrl(zipFileDownloadUrl);
        taskResult.setZipFileId(zipFileId);
        taskResult.setJsonFileName(jsonFileName);
        taskResult.setContainQuery(isContainQuery);
        taskResult.setErrorRecordsFilePath(errorRecordsFilePath);
        return taskResult;
    }

    public double getProgressPercentage() {
        if (sqlTotalBytes == 0) {
            // do nothing and done
            return 100.0D;
        }
        double progress = sqlReadBytes * 100.0D / sqlTotalBytes;
        return Math.min(progress, 100.0D);
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
            log.info("async task result set was saved as JSON file successfully, file name={}", jsonFilePath);
        } catch (IOException e) {
            log.warn("build JSON file failed, errorMessage={}", e.getMessage());
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
            throw new UnexpectedException("write csv file failed");
        }
    }

    private void writeZipFile() {
        try {
            String jsonString = JsonUtils.prettyToJson(csvFileMappers);
            FileUtils.writeStringToFile(new File(String.format("%s/csv_execute_result.json", zipFileRootPath)),
                    jsonString, StandardCharsets.UTF_8, true);
            OdcFileUtil.zip(String.format(zipFileRootPath), String.format("%s.zip", zipFileRootPath));
            log.info("database change task result set was saved as local zip file, file name={}", zipFileId);
            // Public cloud scenario, need to upload files to OSS
            if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
                File tempZipFile = new File(String.format("%s.zip", zipFileRootPath));
                try {
                    String objectName = cloudObjectStorageService.uploadTemp(zipFileId + ".zip", tempZipFile);
                    zipFileDownloadUrl = TaskDownloadUrlsProvider
                            .concatBucketAndObjectName(cloudObjectStorageService.getBucketName(), objectName);
                    log.info("upload database change task result set zip file to OSS, file name={}", zipFileId);
                } catch (Exception exception) {
                    log.warn("upload database change task result set zip file to OSS failed, file name={}", zipFileId);
                    throw new RuntimeException(String.format(
                            "upload database change task result set zip file to OSS failed, file name: %s", zipFileId),
                            exception.getCause());
                } finally {
                    OdcFileUtil.deleteFiles(tempZipFile);
                }
            }
            FileUtils.deleteDirectory(new File(zipFileRootPath));
        } catch (IOException ex) {
            throw new UnexpectedException("build zip file failed");
        }
    }

    private String generateErrorRecord(int index, String sql, String errorMsg) {
        StringBuilder stringBuilder = new StringBuilder();
        String localizedMsg = ErrorCodes.TaskSqlExecuteFailed.getEnglishMessage(new Object[] {index});
        stringBuilder.append(localizedMsg).append(": ").append(sql).append(' ').append(errorMsg);
        return StringUtils.singleLine(stringBuilder.toString()) + "\n";
    }

    private void dynamicDataMasking(SqlExecuteResult result) {
        List<Set<SensitiveColumn>> columns =
                maskingService.getResultSetSensitiveColumns(result.getExecuteSql(), connectionSession);
        if (DataMaskingUtil.isSensitiveColumnExists(columns)) {
            List<Algorithm> algorithms = maskingService.getResultSetMaskingAlgorithmMaskers(columns);
            maskingService.maskRowsUsingAlgorithms(result, algorithms);
        }
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

    private static class RetryResult {
        boolean success;
        String track;
    }

}
