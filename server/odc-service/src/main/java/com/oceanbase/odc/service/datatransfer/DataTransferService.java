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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.ExportOutput;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.UploadFileResult;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datatransfer.loader.ThirdPartyOutputConverter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTask;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTaskContext;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;
import com.oceanbase.odc.service.task.TaskService;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

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
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowInstanceService flowInstanceService;

    @PostConstruct
    public void init() {
        flowInstanceService
                .addDataTransferTaskInitHook(event -> initTransferObjects(event.getTaskId(), event.getConfig()));
    }

    /**
     * create a data transfer task
     *
     * @param bucket This parameter can be any value that can mark the task, eg. taskId
     * @param transferConfig config of the data transfer task
     * @return control handle of the task
     */
    public DataTransferTaskContext create(@NonNull String bucket,
            @NonNull DataTransferConfig transferConfig) throws Exception {
        try {
            // set log path
            Path logPath = Paths.get(taskLogDir, "data-transfer", bucket);

            // clear working directory and create bucket for client mode
            File workingDir = dataTransferAdapter.preHandleWorkDir(transferConfig, bucket,
                    fileManager.getWorkingDir(TaskType.EXPORT, bucket));
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                throw new IllegalStateException("Failed to create working dir, " + workingDir.getAbsolutePath());
            }
            // 目标目录可能已经存在且其中可能存留有导入导出历史脏数据，这里需要清理避免潜在问题，且为了影响最小化，只清理导入导出相关的目录
            String parent = new File(workingDir, "data").getAbsolutePath();
            Arrays.stream(ObjectType.values()).map(ObjectType::getName).forEach(objectName -> {
                File target = new File(parent, objectName);
                if (target.exists() && target.isDirectory()) {
                    boolean deleteRes = FileUtils.deleteQuietly(target);
                    log.info("Delete object directory, dir={}, result={}", target.getAbsolutePath(), deleteRes);
                }
            });
            // inject connection info
            Long connectionId = transferConfig.getConnectionId();
            PreConditions.validArgumentState(connectionId != null, ErrorCodes.BadArgument,
                    new Object[] {"ConnectionId can not be null"}, "ConnectionId can not be null");
            ConnectionConfig connectionConfig = connectionService.getForConnectionSkipPermissionCheck(connectionId);
            ConnectionInfo connectionInfo = connectionConfig.toConnectionInfo();
            connectionInfo.setSchema(transferConfig.getSchemaName());
            transferConfig.setConnectionInfo(connectionInfo);
            injectSysConfig(connectionConfig, transferConfig);

            // set config properties
            transferConfig.setCursorFetchSize(dataTransferProperties.getCursorFetchSize());
            transferConfig.setUsePrepStmts(dataTransferProperties.isUseServerPrepStmts());
            if (dataTransferAdapter.getMaxDumpSizeBytes() != null) {
                transferConfig.setMaxDumpSizeBytes(dataTransferAdapter.getMaxDumpSizeBytes());
            }

            // task placeholder
            DataTransferTask task = DataTransferTask.builder()
                    .adapter(dataTransferAdapter)
                    .creator(authenticationFacade.currentUser())
                    .config(transferConfig)
                    .workingDir(workingDir)
                    .logDir(logPath.toFile())
                    .connectionConfig(connectionConfig)
                    .maskingService(maskingService)
                    .build();
            Future<DataTransferTaskResult> future = executor.submit(task);

            return new DataTransferTaskContext(future, task);

        } catch (Exception e) {
            LOGGER.warn("Failed to init data transfer task.", e);
            throw e;
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
                ExportOutput dumperOutput = new ExportOutput(uploadFile);
                return UploadFileResult.ofExportOutput(fileName, dumperOutput);
            } catch (Exception e) {
                log.warn("Not a valid zip file, file={}", fileName, e);
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
            @NonNull Long databaseId, Set<ObjectType> objectTypes) throws SQLException {
        Database database = databaseService.detail(databaseId);
        if (Objects.isNull(database.getProject())
                && authenticationFacade.currentUser().getOrganizationType() == OrganizationType.TEAM) {
            throw new AccessDeniedException();
        }
        ConnectionConfig connection = database.getDataSource();

        /*
         * only the intersection of two sets can be exported
         */
        Set<ObjectType> supportedObjectTypes = TaskPluginUtil.getDataTransferExtension(connection.getDialectType())
                .getSupportedObjectTypes(connection.toConnectionInfo());
        if (CollectionUtils.isEmpty(objectTypes)) {
            objectTypes = supportedObjectTypes;
        } else {
            objectTypes = SetUtils.intersection(objectTypes, supportedObjectTypes);
        }

        try (DBObjectNameAccessor accessor = DBObjectNameAccessor.getInstance(connection, database.getName())) {
            Map<ObjectType, Set<String>> returnVal = new HashMap<>();
            for (ObjectType objectType : objectTypes) {
                returnVal.putIfAbsent(objectType, accessor.getObjectNames(objectType));
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
                    .withDelimiter(csvConfig.getColumnSeparator())
                    .withEscape(format.toChar("\\"))
                    .withQuote(csvConfig.getColumnDelimiter())
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
            LOGGER.warn("Errors occured when parse CSV file, csvConfig={}", csvConfig, e);
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

    @Transactional(rollbackFor = Exception.class)
    public void initTransferObjects(Long taskId, DataTransferConfig config) {
        if (config.isExportAllObjects() || CollectionUtils.isEmpty(config.getExportDbObjects())) {
            return;
        }
        DataTransferTaskResult result = new DataTransferTaskResult();
        List<ObjectResult> objects = config.getExportDbObjects().stream().map(
                obj -> new ObjectResult(config.getSchemaName(), obj.getObjectName(), obj.getDbObjectType().getName()))
                .collect(Collectors.toList());
        if (config.isTransferDDL()) {
            result.setSchemaObjectsInfo(objects);
        }
        if (config.isTransferData()) {
            List<ObjectResult> tables =
                    objects.stream().filter(obj -> ObjectType.TABLE.getName().equals(obj.getType()))
                            .collect(Collectors.toList());
            result.setDataObjectsInfo(tables);
        }
        taskService.updateResult(taskId, result);
    }

    private void injectSysConfig(ConnectionConfig connectionConfig, DataTransferConfig transferConfig) {
        String sysUserInMeta = connectionConfig.getSysTenantUsername();
        if (StringUtils.isBlank(sysUserInMeta)) {
            log.info("No Sys user setting");
            return;
        }
        connectionConfig.setUsername(sysUserInMeta);
        connectionConfig.setPassword(connectionConfig.getSysTenantPassword());
        if (testSysTenantAccount(connectionConfig)) {
            log.info("Sys user has been approved, connectionId={}", connectionConfig.getId());
            transferConfig.getConnectionInfo().setSysTenantUsername(sysUserInMeta);
            transferConfig.getConnectionInfo().setSysTenantPassword(connectionConfig.getSysTenantPassword());
            return;
        }
        log.info("Access denied, Sys tenant account and password error, connectionId={}, sysUserInConfig={}",
                connectionConfig.getId(), sysUserInMeta);
        transferConfig.getConnectionInfo().setSysTenantUsername(null);
        transferConfig.getConnectionInfo().setSysTenantPassword(null);
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

    private String truncateValue(String val) {
        return val.substring(0, Math.min(val.length(), PREVIEW_PRESERVE_LENGTH));
    }
}
