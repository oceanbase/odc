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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import com.oceanbase.odc.core.shared.PreConditions;
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
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.model.FileMeta;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.DataMaskingUtil;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
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

    private ConnectionSession connectionSession;
    private List<String> sqls;
    private DatabaseChangeParameters parameters;
    // sql execute error records file path
    private String errorRecordsFilePath = null;
    private Integer failCount = 0;
    private Integer successCount = 0;
    private int writeFileSuccessCount = 0;
    private int writeFileFailCount = 0;
    private String zipFileDownloadUrl;
    private String zipFileId;
    private String jsonFileName;
    private boolean isContainQuery = false;

    private List<SqlExecuteResult> queryResultSet = new ArrayList<>();
    private Long startTimestamp = null;
    private boolean abort = false;

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

    private void init(Long userId) {
        List<String> objectIds = parameters.getSqlObjectIds();
        String sqlStr;
        if (StringUtils.isNotEmpty(parameters.getSqlContent())) {
            sqlStr = parameters.getSqlContent();
        } else {
            try {
                sqlStr = readSqlFiles(userId, objectIds);
            } catch (IOException exception) {
                throw new InternalServerError("load async task file failed", exception);
            }
        }
        PreConditions.notEmpty(sqlStr, "sqlStr");
        String delimiter = Objects.isNull(parameters.getDelimiter()) ? ";" : parameters.getDelimiter();
        ConnectionSessionUtil.getSqlCommentProcessor(connectionSession).setDelimiter(delimiter);
        this.sqls = SqlUtils.split(connectionSession, sqlStr, false);
    }

    private String readSqlFiles(Long userId, List<String> objectIds) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String objectId : objectIds) {
            String objectContentStr = objectStorageFacade.loadObjectContentAsString(
                    "async".concat(File.separator).concat(String.valueOf(userId)),
                    objectId);
            /**
             * remove UTF-8 BOM
             */
            byte[] byteSql = objectContentStr.getBytes();
            if (byteSql.length >= 3 && byteSql[0] == (byte) 0xef && byteSql[1] == (byte) 0xbb
                    && byteSql[2] == (byte) 0xbf) {
                objectContentStr = new String(byteSql, 3, byteSql.length - 3);
            }
            sb.append(objectContentStr);
        }
        return sb.toString();
    }

    @Override
    public void run() {
        TaskContextHolder.trace(userId, taskId);
        log.info("Async task  start to run, task id:{}", this.getTaskId());
        log.info("Start read sql content, taskId={}", this.getTaskId());
        init(userId);
        log.info("Read sql content successfully, taskId={}, sqlCount={}", this.getTaskId(), this.sqls.size());
        startTimestamp = System.currentTimeMillis();
        String fileDir = FileManager.generateDir(FileBucket.ASYNC);
        for (int index = 1; index <= sqls.size(); index++) {
            String sql = sqls.get(index - 1);
            log.info("Async sql: {}", sql);
            if (stop) {
                break;
            }
            try {
                List<SqlTuple> sqlTuples = new ArrayList<>();
                sqlTuples.add(SqlTuple.newTuple(sql));
                OdcStatementCallBack statementCallback = new OdcStatementCallBack(sqlTuples, connectionSession, true,
                        parameters.getQueryLimit());
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
                            stmt.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(parameters.getTimeoutMillis()));
                            return statementCallback.doInStatement(stmt);
                        });
                Verify.notEmpty(results, "resultList");
                GeneralSqlType sqlType = parseSqlType(sql);
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
                    queryResultSet.add(executeResult);
                }
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
        try {
            jsonFileName = writeJsonFile(fileDir, queryResultSet);
            FileMeta fileMeta = writeZipFile(fileDir, queryResultSet, cloudObjectStorageService, flowInstanceId);
            zipFileDownloadUrl = fileMeta.getDownloadUrl();
            zipFileId = fileMeta.getFileId();
            writeFileSuccessCount++;
            log.info("Async task end up running, task id: {}", this.getTaskId());
        } catch (Exception e) {
            writeFileFailCount++;
            log.warn("Write async task file failed, task id: {}, error message: {}", this.getTaskId(), e.getMessage());
        } finally {
            TaskContextHolder.clear();
            connectionSession.expire();
        }
    }

    private void addErrorRecordsToFile(int index, String sql, String errorMsg) {
        if (StringUtils.isBlank(this.errorRecordsFilePath)) {
            this.errorRecordsFilePath =
                    FileManager.generateDir(FileBucket.ASYNC) + File.separator + StringUtils.uuid() + ".txt";
        }
        try (FileWriter fw = new FileWriter(this.errorRecordsFilePath, true)) {
            String modifiedErrorMsg = generateErrorRecord(index, sql, errorMsg);
            fw.append(modifiedErrorMsg);
        } catch (IOException ex) {
            log.warn("generate error record failed, sql index={}, sql={}, errorMsg={}", index, sql, errorMsg);
        }
    }

    public boolean isAbort() {
        return this.abort;
    }

    public boolean isDone() {
        // whether all sql execute [successCount + failCount] and result save [writeFileSuccessCount +
        // writeFileFailCount]
        // has greater or equal than sql list size + 1
        // successCount + failCount should be equals to sql list size
        // writeFileSuccessCount + writeFileFailCount should be 0 or 1
        return (successCount + failCount + writeFileSuccessCount + writeFileFailCount) >= (sqls.size() + 1);
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

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public DatabaseChangeParameters getParameters() {
        return parameters;
    }

    public double getProgressPercentage() {
        int totalCount = sqls.size();
        if (totalCount == 0) {
            // do nothing and done
            return 100.0D;
        }
        return (successCount + failCount + writeFileSuccessCount + writeFileFailCount) * 100.0D / (totalCount + 1);
    }

    public static String writeJsonFile(String dir, List<SqlExecuteResult> executeResult) {
        try {
            FileUtils.forceMkdir(new File(dir));
            String fileId = StringUtils.uuid();
            String filePath = dir + String.format("/%s.json", fileId);
            String jsonString = JsonUtils.toJson(executeResult);
            FileUtils.writeStringToFile(new File(filePath), jsonString, StandardCharsets.UTF_8, true);
            log.info("async task result set was saved as JSON file successfully, file name={}", filePath);
            return fileId;
        } catch (IOException e) {
            log.warn("build JSON file failed, errorMessage={}", e.getMessage());
            throw new UnexpectedException("build JSON file failed");
        }
    }

    public static FileMeta writeZipFile(String dir, List<SqlExecuteResult> result,
            CloudObjectStorageService cloudObjectStorageService, long flowInstanceId) {
        try {
            FileUtils.forceMkdir(new File(dir));
            List<CSVExecuteResult> csvFileMappers = new ArrayList<>();
            String fileId = StringUtils.uuid();
            int sequence = 0;
            for (int i = 0; i < result.size(); i++) {
                sequence++;
                SqlExecuteResult sqlExecuteResult = result.get(i);
                if (Objects.isNull(sqlExecuteResult.getRows()) || sqlExecuteResult.getRows().size() == 0) {
                    continue;
                }
                String csv = CSVUtils.buildCSVFormatData(sqlExecuteResult.getColumns(), sqlExecuteResult.getRows());
                String filePath = String.format("%s/%s/%s.csv", dir, fileId, i);
                FileUtils.writeStringToFile(new File(filePath), csv, StandardCharsets.UTF_8, true);

                String executeSql = result.get(i).getExecuteSql();
                String fileName = String.format("%s.csv", i);
                CSVExecuteResult csvExecuteResult = new CSVExecuteResult(sequence, executeSql, fileName);
                csvFileMappers.add(csvExecuteResult);
            }

            // write json file to record CSV file name and its corresponding execute sql
            String jsonString = JsonUtils.prettyToJson(csvFileMappers);
            FileUtils.writeStringToFile(new File(String.format("%s/%s/csv_execute_result.json", dir, fileId)),
                    jsonString,
                    StandardCharsets.UTF_8, true);
            OdcFileUtil.zip(String.format("%s/%s", dir, fileId), String.format("%s/%s.zip", dir, fileId));
            log.info("Async task result set was saved as local zip file successfully, file name={}", fileId);
            String downloadUrl = String.format("/api/v2/flow/flowInstances/%s/tasks/download", flowInstanceId);
            // 公有云场景，需要上传文件到 OSS
            if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
                File tempZipFile = new File(String.format("%s/%s.zip", dir, fileId));
                try {
                    String objectName = cloudObjectStorageService.uploadTemp(fileId + ".zip", tempZipFile);
                    downloadUrl = cloudObjectStorageService.getBucketName() + "/" + objectName;
                    log.info("upload async task result set zip file to OSS successfully, file name={}", fileId);
                } catch (Exception exception) {
                    log.warn("upload async task result set zip file to OSS failed, file name={}", fileId);
                    throw new RuntimeException(String
                            .format("upload async task result set zip file to OSS failed, file name: %s", fileId),
                            exception.getCause());
                } finally {
                    OdcFileUtil.deleteFiles(tempZipFile);
                }
            }
            FileUtils.deleteDirectory(new File(String.format("%s/%s", dir, fileId)));
            return new FileMeta(fileId, downloadUrl);
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
        return ParserUtil.getGeneralSqlType(factory.buildAst(sql).getParseResult());
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
