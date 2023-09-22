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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferTask;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.loader.ThirdPartyOutputConverter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.service.datatransfer.model.UploadFileResult;
import com.oceanbase.odc.service.datatransfer.task.BaseTransferTaskRunner;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTaskContext;
import com.oceanbase.odc.service.datatransfer.task.ExportTaskRunner;
import com.oceanbase.odc.service.datatransfer.task.ImportTaskRunner;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
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
    @Autowired
    private ConnectionTesting connectionTesting;

    /**
     * create a data transfer task
     *
     * @param bucket This parameter can be any value that can mark the task, eg. taskId
     * @param parameter config of the data transfer task
     * @return control handle of the task
     */
    public DataTransferTaskContext create(@NonNull String bucket, DataTransferParameter parameter)
            throws Exception {
        // set log path
        parameter.setLogPath(Paths.get(taskLogDir, "data-transfer", bucket).toString());
        // clear working directory and create bucket for client mode
        File workingDir = dataTransferAdapter.preHandleWorkDir(parameter, bucket,
                fileManager.getWorkingDir(TaskType.EXPORT, bucket));
        if (!workingDir.exists() || !workingDir.isDirectory()) {
            throw new IllegalStateException("Failed to create working dir, " + workingDir.getAbsolutePath());
        }
        // set connection info
        parameter.setWorkingDir(workingDir);
        ConnectionConfig connectionConfig = Objects.requireNonNull(parameter.getConnectionConfig());
        parameter.setConnectionInfo(connectionConfig.simplify());
        // set sys tenant account for ob-loader-dumper
        injectSysConfig(parameter);
        // set jdbc url
        setJdbcUrl(parameter);

        // task placeholder
        Holder<DataTransferTask> taskHolder = new Holder<>();
        BaseTransferTaskRunner runner;
        if (parameter.getTransferType() == DataTransferType.EXPORT) {
            runner = new ExportTaskRunner(parameter, taskHolder, authenticationFacade.currentUser(),
                    dataTransferAdapter, maskingService, dataTransferProperties);
        } else {
            runner = new ImportTaskRunner(parameter, taskHolder, authenticationFacade.currentUser(),
                    dataTransferAdapter, dataTransferProperties);
        }
        Future<DataTransferTaskResult> future = executor.submit(runner);

        return new DataTransferTaskContext(future, taskHolder);
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

    private void injectSysConfig(DataTransferParameter parameter) {
        String sysUserInConfig = parameter.getSysUser();
        if (StringUtils.isBlank(sysUserInConfig)) {
            log.info("No Sys user setting");
            return;
        }
        String sysPasswordInConfig = parameter.getSysPassword();
        ConnectionConfig connectionConfig = parameter.getConnectionConfig();
        connectionConfig.setSysTenantUsername(sysUserInConfig);
        connectionConfig.setSysTenantPassword(sysPasswordInConfig);
        if (testSysTenantAccount(connectionConfig)) {
            log.info("Sys user has been approved, connectionId={}", connectionConfig.getId());
            parameter.getConnectionInfo().setSysTenantUsername(sysUserInConfig);
            parameter.getConnectionInfo().setSysTenantPassword(sysPasswordInConfig);
            return;
        }
        log.info("Access denied, Sys tenant account and password error, connectionId={}, sysUserInConfig={}",
            connectionConfig.getId(), sysUserInConfig);
        parameter.getConnectionInfo().setSysTenantUsername(null);
        parameter.getConnectionInfo().setSysTenantPassword(null);
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

    private void setJdbcUrl(DataTransferParameter transferParameter) {
        ConnectionInfo connectionInfo = transferParameter.getConnectionInfo();

        Map<String, String> jdbcUrlParams = new HashMap<>();
        jdbcUrlParams.put("maxAllowedPacket", "64000000");
        jdbcUrlParams.put("allowMultiQueries", "true");
        jdbcUrlParams.put("connectTimeout", "5000");
        jdbcUrlParams.put("zeroDateTimeBehavior", DEFAULT_ZERO_DATE_TIME_BEHAVIOR);
        jdbcUrlParams.put("noDatetimeStringSync", "true");
        jdbcUrlParams.put("useSSL", "false");
        jdbcUrlParams.put("allowLoadLocalInfile", "false");
        jdbcUrlParams.put("jdbcCompliantTruncation", "false");
        jdbcUrlParams.put("sendConnectionAttributes", "false");

        if (StringUtils.isNotBlank(connectionInfo.getProxyHost())
                && Objects.nonNull(connectionInfo.getProxyPort())) {
            jdbcUrlParams.put("socksProxyHost", connectionInfo.getProxyHost());
            jdbcUrlParams.put("socksProxyPort", connectionInfo.getProxyPort() + "");
        }
        ConnectionExtensionPoint connectionExtension = ConnectionPluginUtil.getConnectionExtension(
                connectionInfo.getConnectType().getDialectType());
        connectionInfo.setJdbcUrl(connectionExtension.generateJdbcUrl(connectionInfo.getHost(),
                connectionInfo.getPort(), transferParameter.getSchemaName(), jdbcUrlParams));
    }

    private String truncateValue(String val) {
        return val.substring(0, Math.min(val.length(), PREVIEW_PRESERVE_LENGTH));
    }
}
