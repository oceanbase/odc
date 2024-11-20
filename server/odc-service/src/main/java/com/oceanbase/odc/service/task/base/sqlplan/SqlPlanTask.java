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
package com.oceanbase.odc.service.task.base.sqlplan;

import java.io.BufferedInputStream;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.CSVUtils;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
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
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
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
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.schedule.job.PublishSqlPlanJobReq;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.initializer.ConsoleTimeoutInitializer;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanTaskResult;
import com.oceanbase.odc.service.task.base.TaskBase;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.dbbrowser.parser.ParserUtil;
import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlPlanTask extends TaskBase<SqlPlanTaskResult> {

    private PublishSqlPlanJobReq parameters;

    private long taskId;

    private InputStream sqlInputStream;

    private ConnectionSession connectionSession;

    private SyncJdbcExecutor executor;

    private SqlPlanTaskResult result;

    private volatile boolean canceled = false;

    private volatile boolean aborted = false;

    private File resultJsonFile;

    private String zipFileRootPath;

    private String errorRecordPath = null;

    private final List<SqlExecuteResult> queryResultSetBuffer = new ArrayList<>();

    private final List<CSVExecuteResult> csvFileMappers = new ArrayList<>();

    public SqlPlanTask() {}

    @Override
    protected void doInit(JobContext context) {
        this.result = new SqlPlanTaskResult();
        this.parameters =
                JobUtils.fromJson(jobContext.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                        PublishSqlPlanJobReq.class);
        JobContext jobContext = getJobContext();
        Map<String, String> jobProperties = jobContext.getJobProperties();
        this.taskId = jobContext.getJobIdentity().getId();
        this.result.setRegion(JobPropertiesUtils.getRegionName(jobProperties));
        this.result.setCloudProvider(JobPropertiesUtils.getCloudProvider(jobProperties));
        this.connectionSession = generateSession();
        this.executor = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        long timeoutUs = TimeUnit.MILLISECONDS.toMicros(parameters.getTimeoutMillis());
        PreConditions.notNull(timeoutUs, "timeoutUs");
        if (timeoutUs < 0) {
            throw new IllegalArgumentException(
                    "Invalid timeout settings, " + parameters.getTimeoutMillis());
        }
        ConnectionInitializer initializer =
                new ConsoleTimeoutInitializer(timeoutUs, connectionSession.getDialectType());
        executor.execute((ConnectionCallback<Void>) con -> {
            initializer.init(con);
            return null;
        });
        initFile();
    }

    @Override
    public boolean start() throws Exception {
        try {
            int index = 0;
            initSqlInputStream();
            SqlStatementIterator sqlIterator =
                    SqlUtils.iterator(connectionSession, sqlInputStream, StandardCharsets.UTF_8);
            while (sqlIterator.hasNext()) {
                if (canceled) {
                    log.info("Accept cancel task request, taskId={}", taskId);
                    break;
                }
                String sql = sqlIterator.next().getStr();
                index++;
                // The retry statement will write the result into the buffer, while executing a new SQL command will
                // clear the buffer.
                queryResultSetBuffer.clear();
                try {
                    boolean success = executeSqlWithRetries(sql);
                    // write all result into json file
                    appendResultToJsonFile(index == 1, !sqlIterator.hasNext());
                    // write result rows into csv file
                    writeCsvFiles(index);
                    if (success) {
                        result.incrementSucceedStatements();
                    } else {
                        log.info("execute sql failed, sql={}", sql);
                        result.incrementFailedStatements();
                        // only write failed record into error records file
                        addErrorRecordsToFile(index, sql);
                        if (parameters.getErrorStrategy() == TaskErrorStrategy.ABORT) {
                            aborted = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.info("execute sql failed, sql={}", sql);
                    result.incrementFailedStatements();
                    addErrorRecordsToFile(index, sql);
                    if (parameters.getErrorStrategy() == TaskErrorStrategy.ABORT) {
                        aborted = true;
                        break;
                    }
                    log.warn("Sql task execution failed, will continue to execute next statement.", e);
                    context.getExceptionListener().onException(e);
                }
            }
            result.setTotalStatements(index);

            // all sql execute csv file list write to zip file
            writeZipFile();
            // upload file to OSS, also contains error record where is non-null
            upload();

            if (aborted) {
                throw new JobException("There exists error sql, and the task is aborted");
            }

            log.info("The sql plan task execute finished, report statistics:total={}, succeed={}, failed={}",
                    result.getTotalStatements(), result.getSucceedStatements(), result.getFailedStatements());
            return true;
        } finally {
            tryCloseInputStream();
        }
    }

    private void initSqlInputStream() {
        if (CollectionUtils.isEmpty(parameters.getSqlObjectIds()) && StringUtils.isBlank(parameters.getSqlContent())) {
            throw new UnexpectedException("Sql content and sql object id can not be null at the same time.");
        }
        this.sqlInputStream = new ByteArrayInputStream(new byte[0]);

        if (StringUtils.isNotBlank(parameters.getSqlContent())) {
            byte[] bytes = parameters.getSqlContent().getBytes();
            this.sqlInputStream = new ByteArrayInputStream(bytes);
            return;
        }

        CloudObjectStorageService cloudObjectStorageService = context.getSharedStorage();
        if (Objects.isNull(cloudObjectStorageService) || !cloudObjectStorageService.supported()) {
            log.warn("Cloud object storage service not supported.");
            throw new UnexpectedException("Cloud object storage service not supported");
        }

        for (String sqlObjectId : parameters.getSqlObjectIds()) {
            try {
                BufferedInputStream current =
                        new BufferedInputStream(cloudObjectStorageService.getObject(sqlObjectId));
                // remove UTF-8 BOM if exists
                current.mark(3);
                byte[] byteSql = new byte[3];
                if (current.read(byteSql) >= 3 && byteSql[0] == (byte) 0xef && byteSql[1] == (byte) 0xbb
                        && byteSql[2] == (byte) 0xbf) {
                    current.reset();
                    current.skip(3);
                } else {
                    current.reset();
                }
                sqlInputStream = new SequenceInputStream(sqlInputStream, current);
            } catch (IOException e) {
                log.warn("Parsing sql script file failed, objectName={}, errorReason={}", sqlObjectId,
                        ExceptionUtils.getSimpleReason(e));
                throw new InternalServerError("load database change task file failed", e);
            }
        }
    }

    @Override
    public void stop() {
        canceled = true;
    }

    @Override
    public void close() {
        tryExpireConnectionSession();
    }

    @Override
    public double getProgress() {
        if (result.getTotalStatements() == 0) {
            return 0.0;
        }
        return (double) result.getFinishedStatements() / result.getTotalStatements();
    }

    @Override
    public SqlPlanTaskResult getTaskResult() {
        return this.result;
    }

    private boolean executeSqlWithRetries(String sql) {
        int executeTime = 0;
        GeneralSqlType sqlType = parseSqlType(sql);
        while (executeTime <= parameters.getRetryTimes() && !canceled) {
            OdcStatementCallBack statementCallback = getOdcStatementCallBack(sql);
            try {
                List<JdbcGeneralResult> results =
                        executor.execute((StatementCallback<List<JdbcGeneralResult>>) stmt -> {
                            stmt.setQueryTimeout(
                                    (int) TimeUnit.MILLISECONDS.toSeconds(parameters.getTimeoutMillis()));
                            return statementCallback.doInStatement(stmt);
                        });
                Verify.notEmpty(results, "resultList");
                for (JdbcGeneralResult result : results) {
                    // one sql, one execute result
                    SqlExecuteResult executeResult = new SqlExecuteResult(result);
                    if (sqlType == GeneralSqlType.DQL) {
                        // todo: weather need data masking
                        log.info("Success execute DQL sql, result={}", executeResult.getRows());
                    }
                    queryResultSetBuffer.add(executeResult);

                    if (result.getStatus() != SqlExecuteStatus.SUCCESS) {
                        // differ from the query timeout retries, this means sql executed but failed.
                        log.warn("Error occurs when executing sql={}, error message={}", sql,
                                executeResult.getTrack());
                        return false;
                    } else {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (executeTime < parameters.getRetryTimes()) {
                    log.warn(String.format("Will retry for the %sth time in %s seconds...", executeTime + 1,
                            parameters.getRetryIntervalMillis() / 1000));
                    try {
                        Thread.sleep(parameters.getRetryIntervalMillis());
                    } catch (InterruptedException ex) {
                        log.warn("sql task execution is interrupted, task will exit", ex);
                        canceled = true;
                        return false;
                    }
                }
                throw e;
            } finally {
                executeTime++;
            }
        }
        log.info("Sql task execution succeed.");
        return true;
    }


    private ConnectionSession generateSession() {
        ConnectionConfig connectionConfig = JobUtils.fromJson(
                jobContext.getJobParameters().get(JobParametersKeyConstants.CONNECTION_CONFIG), ConnectionConfig.class);
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        sessionFactory.setSessionTimeoutMillis(parameters.getTimeoutMillis());
        ConnectionSession connectionSession = sessionFactory.generateSession();
        if (connectionSession.getDialectType().isOracle()) {
            ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession, this.parameters.getSessionTimeZone());
        }
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true, true);
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);
        ConnectionSessionUtil.getSqlCommentProcessor(connectionSession).setDelimiter(parameters.getDelimiter());
        return connectionSession;
    }

    private void initFile() {
        try {
            String fileRootDir = FileManager.generateDir(FileBucket.ASYNC);
            String fileName = StringUtils.uuid();
            String filePath = String.format("%s/%s.json", fileRootDir, fileName);
            this.resultJsonFile = new File(filePath);
            this.zipFileRootPath = String.format("%s/%s", fileRootDir, StringUtils.uuid());
        } catch (Exception e) {
            throw new InternalServerError("create sql plan task file dir failed", e);
        }
    }



    private OdcStatementCallBack getOdcStatementCallBack(String sql) {
        List<SqlTuple> sqlTuples = Collections.singletonList(SqlTuple.newTuple(sql));
        OdcStatementCallBack statementCallback =
                new OdcStatementCallBack(sqlTuples, connectionSession, true, parameters.getQueryLimit());
        statementCallback.setMaxCachedLines(0);
        statementCallback.setMaxCachedSize(0);
        statementCallback.setDbmsoutputMaxRows(0);
        return statementCallback;
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
        log.info("Close sql input stream.");
        if (Objects.nonNull(sqlInputStream)) {
            try {
                sqlInputStream.close();
            } catch (Exception e) {
                // eat exception
            }
        }
    }


    private void appendResultToJsonFile(boolean start, boolean end) {
        try {
            if (start) {
                FileUtils.writeStringToFile(resultJsonFile, "[", StandardCharsets.UTF_8, true);
            }
            if (!queryResultSetBuffer.isEmpty()) {
                if (!start) {
                    FileUtils.writeStringToFile(resultJsonFile, ",", StandardCharsets.UTF_8, true);
                }
                String jsonString = JsonUtils.toJson(queryResultSetBuffer);
                FileUtils.writeStringToFile(resultJsonFile, jsonString.substring(1, jsonString.length() - 1),
                        StandardCharsets.UTF_8, true);
            }
            if (end) {
                FileUtils.writeStringToFile(resultJsonFile, "]", StandardCharsets.UTF_8, true);
            }
            log.info("async task result set was saved as JSON file successfully, file name={}",
                    resultJsonFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("build JSON file failed, errorMessage={}", e.getMessage());
            throw new UnexpectedException("build JSON file failed");
        }
    }

    private void writeCsvFiles(int csvFileIndex) {
        try {
            for (SqlExecuteResult result : queryResultSetBuffer) {
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
            File file = new File(String.format("%s/csv_execute_result.json", zipFileRootPath));
            FileUtils.writeStringToFile(file, jsonString, StandardCharsets.UTF_8, true);
            String zipFileName = String.format("%s.zip", zipFileRootPath);
            OdcFileUtil.zip(zipFileRootPath, zipFileName);
            log.info("sql plan task result set was saved as local zip file, file name={}", zipFileName);
            FileUtils.deleteDirectory(new File(zipFileRootPath));
        } catch (IOException ex) {
            throw new UnexpectedException("build zip file failed");
        }
    }

    private void upload() {
        // upload zip
        String zipFilePath = String.format("%s.zip", zipFileRootPath);
        this.result.setCsvResultSetZipDownloadUrl(uploadToOSS(zipFilePath));

        // upload sql execute json file
        this.result.setSqlExecuteJsonFileDownloadUrl(uploadToOSS(resultJsonFile.getPath()));

        // upload error record
        if (errorRecordPath != null) {
            this.result.setErrorRecordsFileDownloadUrl(uploadToOSS(errorRecordPath));
        }
    }

    private String uploadToOSS(String filePath) {
        // Public cloud scenario, need to upload files to OSS
        CloudObjectStorageService cloudObjectStorageService = context.getSharedStorage();
        if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
            File file = new File(filePath);
            String ossAddress;
            try {
                String objectName = cloudObjectStorageService.upload(file.getName(), file);
                ossAddress = String.valueOf(cloudObjectStorageService.generateDownloadUrl(objectName));
                log.info("Upload sql plan task result to cloud object storage successfully, objectName={}", objectName);
            } catch (Exception exception) {
                throw new RuntimeException(String.format(
                        "failed to upload sql plan task result file to OSS, file name: %s", file.getName()),
                        exception.getCause());
            } finally {
                OdcFileUtil.deleteFiles(file);
            }
            return ossAddress;
        }
        return null;
    }

    private void addErrorRecordsToFile(int index, String sql) {
        for (SqlExecuteResult result : queryResultSetBuffer) {
            if (result.getStatus() != SqlExecuteStatus.FAILED) {
                continue;
            }
            if (StringUtils.isBlank(this.errorRecordPath)) {
                this.errorRecordPath =
                        FileManager.generateDir(FileBucket.ASYNC) + File.separator + StringUtils.uuid() + "_error.txt";
            }
            try (FileWriter fw = new FileWriter(this.errorRecordPath, true)) {
                String modifiedErrorMsg = generateErrorRecord(index, sql, result.getTrack());
                fw.append(modifiedErrorMsg);
                this.result.addFailedRecord(modifiedErrorMsg);
            } catch (IOException ex) {
                log.warn("generate error record failed, sql index={}, sql={}, errorMsg={}", index, sql,
                        result.getTrack());
            }
        }
    }

    private String generateErrorRecord(int index, String sql, String errorMsg) {
        StringBuilder stringBuilder = new StringBuilder();
        String localizedMsg = ErrorCodes.TaskSqlExecuteFailed.getEnglishMessage(new Object[] {index});
        stringBuilder.append(localizedMsg).append(": ").append(sql).append(' ').append(errorMsg);
        return StringUtils.singleLine(stringBuilder.toString()) + "\n";
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
