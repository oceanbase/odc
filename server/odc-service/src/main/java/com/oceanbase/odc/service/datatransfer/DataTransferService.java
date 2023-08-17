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
package com.oceanbase.odc.service.datatransfer;

import static com.oceanbase.odc.core.shared.constant.OdcConstants.DEFAULT_ZERO_DATE_TIME_BEHAVIOR;
import static com.oceanbase.odc.service.datatransfer.model.DataTransferConstants.LOG_PATH_NAME;
import static com.oceanbase.tools.loaddump.common.constants.Constants.JdbcConsts.JDBC_URL_USE_SERVER_PREP_STMTS;
import static com.oceanbase.tools.loaddump.common.constants.Constants.JdbcConsts.JDBC_URL_ZERO_DATETIME_BEHAVIOR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.loader.ThirdPartyOutputConverter;
import com.oceanbase.odc.service.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.service.datatransfer.model.CsvConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.service.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.datatransfer.model.UploadFileResult;
import com.oceanbase.odc.service.datatransfer.task.BaseDataTransferTask;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTaskContext;
import com.oceanbase.odc.service.datatransfer.task.ExportDataTransferTask;
import com.oceanbase.odc.service.datatransfer.task.ImportDataTransferTask;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.manager.session.SessionProperties;
import com.oceanbase.tools.loaddump.parser.record.Record;
import com.oceanbase.tools.loaddump.parser.record.csv.CsvFormat;
import com.oceanbase.tools.loaddump.parser.record.csv.CsvRecordParser;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DataTransferService}
 *
 * @author yh263208
 * @date 2022-06-29 14:02
 * @since ODC_release_3.4.0
 */
@Slf4j
@Service
@Validated
@SkipAuthorize("permission check inside")
public class DataTransferService {
    private static final Logger logger = LoggerFactory.getLogger("DataTransferLogger");
    public static final String CLIENT_DIR_PREFIX = "export_";
    public static final int PREVIEW_PRESERVE_LENGTH = 1024;
    @Autowired
    @Qualifier("loaderdumperExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private ConnectionTesting connectionTesting;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private LocalFileManager fileManager;
    @Value("${odc.log.directory:./log}")
    private String taskLogDir;
    @Autowired
    private DataTransferAdapter dataTransferAdapter;
    @Autowired
    private DataMaskingService maskingService;
    @Autowired
    private DataTransferProperties dataTransferProperties;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    /**
     * create a data transfer task
     *
     * @param bucket This parameter can be any value that can mark the task, eg. taskId
     * @param transferConfig config of the data transfer task
     * @return control handle of the task
     */
    public DataTransferTaskContext create(@NonNull String bucket,
            @NonNull DataTransferConfig transferConfig) throws Exception {
        File workingDir = fileManager.getWorkingDir(TaskType.EXPORT, bucket);
        DataTransferType transferType = transferConfig.getTransferType();
        workingDir = this.dataTransferAdapter.preHandleWorkDir(transferConfig, bucket, workingDir);
        // 目标目录可能已经存在且其中可能存留有导入导出历史脏数据，这里需要清理避免潜在问题，且为了影响最小化，只清理导入导出相关的目录
        String parent = new File(workingDir, "data").getAbsolutePath();
        Arrays.stream(ObjectType.values()).map(ObjectType::getName).forEach(objectName -> {
            File target = new File(parent, objectName);
            if (target.exists() && target.isDirectory()) {
                boolean deleteRes = FileUtils.deleteQuietly(target);
                log.info("Delete object directory, dir={}, result={}", target.getAbsolutePath(), deleteRes);
            }
        });
        File logDir = new File(taskLogDir + "/data-transfer/" + bucket);
        if (!logDir.exists()) {
            FileUtils.forceMkdir(logDir);
        }

        ThreadContext.put(LOG_PATH_NAME, logDir.toString());
        setSessionProperties();

        try {
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                throw new IllegalStateException("Failed to create working dir, " + workingDir.getAbsolutePath());
            }
            ConnectionConfig connectionConfig = transferConfig.getConnectionConfig();
            if (connectionConfig == null) {
                Long connectionId = transferConfig.getConnectionId();
                PreConditions.validArgumentState(connectionId != null, ErrorCodes.BadArgument,
                        new Object[] {"ConnectionId can not be null"}, "ConnectionId can not be null");
                connectionConfig = connectionService.getForConnectionSkipPermissionCheck(connectionId);
            }
            injectSysConfig(connectionConfig, transferConfig);

            boolean transferData = transferConfig.isTransferData();
            boolean transferSchema = transferConfig.isTransferDDL();
            if (transferType == DataTransferType.IMPORT) {
                List<String> importFileNames = transferConfig.getImportFileName();
                if (transferConfig.isNotObLoaderDumperCompatible()) {
                    copyImportScripts(importFileNames, transferConfig.getDataTransferFormat(), workingDir);
                } else {
                    copyImportZip(importFileNames, workingDir);
                }
                BaseParameterFactory<LoadParameter> factory =
                        new LoadParameterFactory(workingDir, logDir, connectionConfig);
                LoadParameter parameter = factory.generate(transferConfig);
                ImportDataTransferTask transferTask;
                try {
                    transferTask = new ImportDataTransferTask(parameter, transferData, transferSchema);
                } catch (Exception e) {
                    logger.warn("Failed to init load task, reason : {}", e.getMessage(), e);
                    throw e;
                }
                return BaseDataTransferTask.start(executor, transferTask);
            } else if (transferType == DataTransferType.EXPORT) {
                BaseParameterFactory<DumpParameter> factory = new DumpParameterFactory(workingDir, logDir,
                        connectionConfig, dataTransferAdapter.getMaxDumpSizeBytes(),
                        dataTransferProperties.getCursorFetchSize(), maskingService);
                DumpParameter parameter = factory.generate(transferConfig);
                ExportDataTransferTask transferTask;
                try {
                    transferTask = new ExportDataTransferTask(
                            parameter, transferData, transferSchema, dataTransferAdapter);
                } catch (Exception e) {
                    logger.warn("Failed to init dump task, reason : {}", e.getMessage(), e);
                    throw e;
                }
                transferTask.setMergeSchemaFiles(transferConfig.isMergeSchemaFiles());
                return BaseDataTransferTask.start(executor, transferTask);
            }
            throw new IllegalArgumentException("Illegal transfer type " + transferType);
        } finally {
            ThreadContext.clearAll();
        }
    }

    public UploadFileResult getMetaInfo(@NonNull String fileName) throws IOException {
        File uploadFile = fileManager.findByName(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, fileName).orElseThrow(
                () -> new FileNotFoundException("File not found"));
        if (!uploadFile.exists() || !uploadFile.isFile()) {
            throw new IllegalArgumentException("Target is not a file or does not exist, " + fileName);
        }

        // If the file is from third party like PL/SQL, this will convert it compatible with ob-loader.
        ThirdPartyOutputConverter.convert(uploadFile);

        String uploadFileName = uploadFile.getName();
        if (StringUtils.endsWithIgnoreCase(uploadFileName, ".zip")) {
            // 疑似 zip 压缩文件，需要进一步确认是否合法
            try {
                DumperOutput dumperOutput = new DumperOutput(uploadFile);
                return UploadFileResult.ofDumperOutput(fileName, dumperOutput);
            } catch (Exception e) {
                log.warn("Not a valid zip file, file={}", fileName, e);
                logger.warn("Not a valid zip file, file={}", fileName, e);
                return UploadFileResult.ofFail(ErrorCodes.ImportInvalidFileType, new Object[] {uploadFileName});
            }
        } else if (StringUtils.endsWithIgnoreCase(uploadFileName, ".csv")) {
            return UploadFileResult.ofCsv(fileName);
        } else if (StringUtils.endsWithIgnoreCase(uploadFileName, ".sql")
                || StringUtils.endsWithIgnoreCase(uploadFileName, ".txt")) {
            return UploadFileResult.ofSql(fileName);
        }
        return UploadFileResult.ofFail(ErrorCodes.ImportInvalidFileType, new Object[] {uploadFileName});
    }

    public UploadFileResult upload(@NonNull MultipartFile uploadFile) throws IOException {
        return upload(uploadFile.getInputStream(), uploadFile.getOriginalFilename());
    }

    public Map<ObjectType, Set<String>> getExportObjectNames(
            @NonNull Long databaseId, Set<ObjectType> objectTypes) {
        Database database = databaseService.detail(databaseId);
        if (Objects.isNull(database.getProject())
                && authenticationFacade.currentUser().getOrganizationType() == OrganizationType.TEAM) {
            throw new AccessDeniedException();
        }
        ConnectionConfig connection = database.getDataSource();


        try (DBObjectNameAccessor accessor = DBObjectNameAccessor.getInstance(connection, database.getName())) {
            Map<ObjectType, Set<String>> returnVal = new HashMap<>();
            if (CollectionUtils.isNotEmpty(objectTypes)) {
                for (ObjectType objectType : objectTypes) {
                    returnVal.putIfAbsent(objectType, accessor.getObjectNames(objectType));
                }
                return returnVal;
            }
            returnVal.putIfAbsent(ObjectType.TABLE, accessor.getTableNames());
            returnVal.putIfAbsent(ObjectType.VIEW, accessor.getViewNames());
            returnVal.putIfAbsent(ObjectType.PROCEDURE, accessor.getProcedureNames());
            returnVal.putIfAbsent(ObjectType.FUNCTION, accessor.getFunctionNames());
            if (connection.getDialectType() == DialectType.OB_ORACLE) {
                returnVal.putIfAbsent(ObjectType.TRIGGER, accessor.getTriggerNames());
                returnVal.putIfAbsent(ObjectType.SEQUENCE, accessor.getSequenceNames());
                returnVal.putIfAbsent(ObjectType.SYNONYM, accessor.getSynonymNames());
                returnVal.putIfAbsent(ObjectType.PUBLIC_SYNONYM, accessor.getPublicSynonymNames());
                returnVal.putIfAbsent(ObjectType.PACKAGE, accessor.getPackageNames());
                returnVal.putIfAbsent(ObjectType.PACKAGE_BODY, accessor.getPackageBodyNames());
            } else if (Objects.nonNull(connection.getDialectType()) && connection.getDialectType().isOBMysql()
                    && VersionUtils.isGreaterThanOrEqualsTo(accessor.getDBVersion(), "4.0.0")) {
                returnVal.putIfAbsent(ObjectType.SEQUENCE, accessor.getSequenceNames());
            }
            return returnVal;
        }
    }

    public List<CsvColumnMapping> getCsvFileInfo(CsvConfig csvConfig) throws IOException {
        Optional<File> optional =
                fileManager.findByName(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, csvConfig.getFileName());
        if (!optional.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_FILE, "fileName", csvConfig.getFileName());
        }
        List<CsvColumnMapping> mappingList = new LinkedList<>();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(optional.get()),
                Charset.forName(csvConfig.getEncoding().getAlias()))) {
            CsvFormat format = CsvFormat.DEFAULT;
            format = format
                    .withDelimiter(format.toChar(csvConfig.getColumnSeparator()))
                    .withEscape(format.toChar("\\"))
                    .withQuote(format.toChar(csvConfig.getColumnDelimiter()))
                    .withRecordSeparator(format.toChar(csvConfig.getLineSeparator()))
                    .withNullString("\\N")
                    .withEmptyString("")
                    .withIgnoreEmptyLines(true)
                    .withIgnoreSurroundingSpaces(true);
            if (csvConfig.isBlankToNull()) {
                format = format.withEmptyReplacer("");
            }
            CsvRecordParser parser = format.parse(reader);
            Iterator<Record> iter = parser.iterator();
            if (csvConfig.isSkipHeader()) {
                if (iter.hasNext()) {
                    Record csvHeader = iter.next();
                    for (int i = 0; i < csvHeader.size(); i++) {
                        CsvColumnMapping csvMapping = new CsvColumnMapping();
                        csvMapping.setSrcColumnName(csvHeader.get(i));
                        csvMapping.setSrcColumnPosition(i);
                        mappingList.add(csvMapping);
                    }
                }
                if (iter.hasNext()) {
                    Record firstLine = iter.next();
                    int size = Math.min(mappingList.size(), firstLine.size());
                    for (int i = 0; i < size; i++) {
                        CsvColumnMapping csvMapping = mappingList.get(i);
                        csvMapping.setFirstLineValue(truncateValue(firstLine.get(i)));
                    }
                }
            } else {
                if (iter.hasNext()) {
                    Record line = iter.next();
                    for (int i = 0; i < line.size(); i++) {
                        CsvColumnMapping csvMapping = new CsvColumnMapping();
                        csvMapping.setSrcColumnPosition(i);
                        csvMapping.setSrcColumnName("column" + (i + 1));
                        csvMapping.setFirstLineValue(truncateValue(line.get(i)));
                        mappingList.add(csvMapping);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Errors occured when parse CSV file, csvConfig={}", csvConfig, e);
            logger.warn("Errors occured when parse CSV file, csvConfig={}", csvConfig, e);
            throw e;
        }
        return mappingList;
    }

    public UploadFileResult upload(@NonNull InputStream inputStream, @NonNull String originalFilename)
            throws IOException {
        String fileName = "import_upload_" + System.currentTimeMillis() + "_" + originalFilename;
        int fileSize = fileManager.copy(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, inputStream, () -> fileName);
        log.info("Upload file successfully, fileName={}, fileSize={} Bytes", fileName, fileSize);
        return getMetaInfo(fileName);
    }

    private void injectSysConfig(ConnectionConfig connectionConfig, DataTransferConfig transferConfig) {
        if (StringUtils.isBlank(transferConfig.getSysUser())) {
            log.info("No Sys user setting, connectionId={}", connectionConfig.getId());
            logger.info("No Sys user setting, connectionId={}", connectionConfig.getId());
            connectionConfig.setSysTenantUsername(null);
            connectionConfig.setSysTenantPassword(null);
            return;
        }
        String sysUserInMeta = connectionConfig.getSysTenantUsername();
        String sysPasswdInMeta = connectionConfig.getSysTenantPassword();
        String sysUserInConfig = transferConfig.getSysUser();
        String sysPasswdInConfig = transferConfig.getSysPassword();
        if (sysPasswdInConfig == null) {
            if (sysPasswdInMeta == null) {
                log.info("No password for sys, connectionId={}", connectionConfig.getId());
                logger.info("No password for sys, connectionId={}", connectionConfig.getId());
                return;
            }
            Validate.isTrue(sysUserInConfig.equals(sysUserInMeta), "Sys user is illegal");
            if (!testSysTenantAccount(connectionConfig)) {
                log.warn("Access denied, Sys tenant account and password error, connectionId={}, sysUserInMeta={}",
                        connectionConfig.getId(), sysUserInMeta);
                logger.warn(
                        "Access denied, Sys tenant account and password error, connectionId={}, sysUserInMeta={}",
                        connectionConfig.getId(), sysUserInMeta);
                throw new IllegalStateException("AccessDenied, " + sysUserInMeta);
            }
            return;
        }
        connectionConfig.setSysTenantUsername(sysUserInConfig);
        connectionConfig.setSysTenantPassword(sysPasswdInConfig);
        if (testSysTenantAccount(connectionConfig)) {
            log.info("Sys user has been approved, connectionId={}", connectionConfig.getId());
            logger.info("Sys user has been approved, connectionId={}", connectionConfig.getId());
            return;
        }
        log.info("Access denied, Sys tenant account and password error, connectionId={}, sysUserInConfig={}",
                connectionConfig.getId(), sysUserInConfig);
        logger.info("Access denied, Sys tenant account and password error, connectionId={}, sysUserInConfig={}",
                connectionConfig.getId(), sysUserInConfig);
        connectionConfig.setSysTenantUsername(null);
        connectionConfig.setSysTenantPassword(null);
    }

    private boolean testSysTenantAccount(ConnectionConfig connectionConfig) {
        TestConnectionReq req = TestConnectionReq.fromConnection(connectionConfig, ConnectionAccountType.SYS_READ);
        try {
            return connectionTesting.test(req).isActive();
        } catch (Exception e) {
            // eat exp
            return false;
        }
    }

    private void copyImportScripts(List<String> fileNames, DataTransferFormat format, File destDir)
            throws IOException {
        Validate.isTrue(CollectionUtils.isNotEmpty(fileNames), "No script found");
        Validate.notNull(format, "DataTransferFormat can not be null");
        if (DataTransferFormat.CSV.equals(format) && fileNames.size() > 1) {
            log.warn("Multiple files for CSV format is invalid, importFileNames={}", fileNames);
            logger.warn("Multiple files for CSV format is invalid, importFileNames={}", fileNames);
            throw new IllegalArgumentException("Multiple files isn't accepted for CSV format");
        }
        for (String fileName : fileNames) {
            Optional<File> importFile =
                    fileManager.findByName(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, fileName);
            File from = importFile.orElseThrow(() -> new FileNotFoundException("File not found, " + fileName));
            File dest = new File(destDir.getAbsolutePath() + File.separator + from.getName());
            try (InputStream inputStream = from.toURI().toURL().openStream();
                    OutputStream outputStream = new FileOutputStream(dest)) {
                IOUtils.copy(inputStream, outputStream);
            }
            log.info("Copy script to working dir, from={}, dest={}", from.getAbsolutePath(), dest.getAbsolutePath());
        }
    }

    private void copyImportZip(List<String> fileNames, File destDir) throws IOException {
        if (fileNames == null || fileNames.size() != 1) {
            log.warn("Single zip file is available, importFileNames={}", fileNames);
            logger.warn("Single zip file is available, importFileNames={}", fileNames);
            throw new IllegalArgumentException("Single zip file is available");
        }
        String fileName = fileNames.get(0);
        Optional<File> uploadFile = fileManager.findByName(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, fileName);
        File from = uploadFile.orElseThrow(() -> new FileNotFoundException("File not found, " + fileName));
        File dest = new File(destDir.getAbsolutePath() + File.separator + "data");
        FileUtils.forceMkdir(dest);
        DumperOutput dumperOutput = new DumperOutput(from);
        dumperOutput.toFolder(dest);
        log.info("Unzip file to working dir, from={}, dest={}", from.getAbsolutePath(), dest.getAbsolutePath());
    }

    private String truncateValue(String val) {
        return val.substring(0, Math.min(val.length(), PREVIEW_PRESERVE_LENGTH));
    }

    private void setSessionProperties() {
        SessionProperties.setString(JDBC_URL_USE_SERVER_PREP_STMTS, dataTransferProperties.getUseServerPrepStmts());
        SessionProperties.setString(JDBC_URL_ZERO_DATETIME_BEHAVIOR, DEFAULT_ZERO_DATE_TIME_BEHAVIOR);
    }
}
