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

package com.oceanbase.odc.service.datatransfer.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.MaskValueType;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferJob;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.DumpDBObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.ExportOutput;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.datatransfer.DataTransferAdapter;
import com.oceanbase.odc.service.datatransfer.LocalFileManager;
import com.oceanbase.odc.service.datatransfer.dumper.SchemaMergeOperator;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.Manifest;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;
import com.oceanbase.tools.loaddump.utils.SerializeUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class DataTransferTask implements Callable<DataTransferTaskResult> {
    private static final Set<String> OUTPUT_FILTER_FILES = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private final DataMaskingService maskingService;
    private final User creator;
    private final DataTransferConfig config;
    private final DataTransferAdapter adapter;
    private final File workingDir;
    private final File logDir;
    private final ConnectionConfig connectionConfig;

    @Getter
    private DataTransferJob job;

    @Override
    public DataTransferTaskResult call() throws Exception {
        try {
            TraceContextHolder.put(DataTransferConstants.LOG_PATH_NAME, logDir.getPath());
            SecurityContextUtils.setCurrentUser(creator);

            List<URL> inputs = Collections.emptyList();
            if (config.getTransferType() == DataTransferType.IMPORT) {
                inputs = copyInputFiles();
                loadManifest();
            } else {
                setMaskConfig();
            }

            job = TaskPluginUtil
                    .getDataTransferExtension(config.getConnectionInfo().getConnectType().getDialectType())
                    .generate(config, workingDir, logDir, inputs);

            DataTransferTaskResult result = job.call();

            validateSuccessful(result);

            if (config.getTransferType() == DataTransferType.IMPORT) {
                clearWorkingDir();
            } else {
                if (config.getDataTransferFormat() != DataTransferFormat.SQL) {
                    // save csv config to MANIFEST
                    Path manifest = Paths.get(workingDir.getPath(), "data", ExportOutput.MANIFEST);
                    SerializeUtils.serializeObjectByKryo(new Manifest(getDumpParameterForManifest()),
                            manifest.toString());
                }
                handleOutput(result);
            }

            return result;

        } catch (Exception e) {
            log.warn("Failed to run data transfer task.", e);
            LOGGER.warn("Failed to run data transfer task.", e);
            // clean up files on exception, no matter what type the task is
            clearWorkingDir();
            throw e;

        } finally {
            TraceContextHolder.clear();
            SecurityContextUtils.clear();
        }
    }

    private List<URL> copyInputFiles() throws Exception {
        /*
         * move import files
         */
        List<String> importFileNames = config.getImportFileName();
        if (config.isCompressed()) {
            ExportOutput exportOutput = copyImportZip(importFileNames, workingDir);
            List<DataTransferObject> objects = new ArrayList<>();
            List<DumpDBObject> dumpDbObjects = exportOutput.getDumpDbObjects();
            List<URL> inputs = new ArrayList<>();
            for (DumpDBObject dbObject : dumpDbObjects) {
                dbObject.getOutputFiles().forEach(abstractOutputFile -> {
                    DataTransferObject transferObject = new DataTransferObject();
                    transferObject.setDbObjectType(dbObject.getObjectType());
                    transferObject.setObjectName(abstractOutputFile.getObjectName());
                    objects.add(transferObject);
                    inputs.add(abstractOutputFile.getUrl());
                });
            }
            /*
             * set whiteList for ob-loader
             */
            config.setExportDbObjects(objects);
            return inputs;
        } else {
            return copyImportScripts(importFileNames, config.getDataTransferFormat(), workingDir);
        }
    }

    private void loadManifest() {
        if (config.getDataTransferFormat() == DataTransferFormat.SQL || !config.isCompressed()) {
            return;
        }
        // load csv config from MANIFEST
        try {
            Manifest manifest = SerializeUtils.deserializeObjectByKryo(
                    Paths.get(workingDir.getPath(), "data", ExportOutput.MANIFEST).toString());
            replaceManifestIntoParameter(manifest);
        } catch (Exception e) {
            LOGGER.info("Failed to deserialize MANIFEST.bin, will use default csv config. Reason: {}",
                    e.getMessage());
            config.setCsvConfig(new CsvConfig());
        }
    }

    private void setMaskConfig() throws Exception {
        String schemaName = config.getSchemaName();
        Map<String, List<String>> tableName2ColumnNames = new HashMap<>();
        ConnectionSession connectionSession = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
            List<String> tableNames;
            if (config.isExportAllObjects()) {
                tableNames = accessor.showTablesLike(schemaName, null).stream()
                        .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                        .collect(Collectors.toList());
            } else {
                tableNames = config.getExportDbObjects().stream()
                        .filter(o -> Objects.equals(ObjectType.TABLE, o.getDbObjectType()))
                        .map(DataTransferObject::getObjectName)
                        .collect(Collectors.toList());
            }
            for (String tableName : tableNames) {
                List<String> tableColumns = accessor.listTableColumns(schemaName, tableName).stream()
                        .map(DBTableColumn::getName).collect(Collectors.toList());
                tableName2ColumnNames.put(tableName, tableColumns);
            }
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }

        Map<SensitiveColumn, MaskingAlgorithm> sensitiveColumn2Algorithm = maskingService
                .listColumnsAndMaskingAlgorithm(config.getDatabaseId(), tableName2ColumnNames.keySet());
        if (sensitiveColumn2Algorithm.isEmpty()) {
            return;
        }
        Map<TableColumn, MaskingAlgorithm> column2Algorithm = sensitiveColumn2Algorithm.keySet().stream()
                .collect(Collectors.toMap(c -> new TableColumn(c.getTableName(), c.getColumnName()),
                        sensitiveColumn2Algorithm::get, (c1, c2) -> c1));
        DataMaskerFactory maskerFactory = new DataMaskerFactory();
        Map<TableIdentity, Map<String, AbstractDataMasker>> maskConfigMap = new HashMap<>();
        for (String tableName : tableName2ColumnNames.keySet()) {
            Map<String, AbstractDataMasker> column2Masker = new HashMap<>();
            for (String columnName : tableName2ColumnNames.get(tableName)) {
                MaskingAlgorithm algorithm = column2Algorithm.get(new TableColumn(tableName, columnName));
                if (Objects.isNull(algorithm)) {
                    continue;
                }
                MaskConfig maskConfig = MaskingAlgorithmUtil.toSingleFieldMaskConfig(algorithm, columnName);
                AbstractDataMasker masker =
                        maskerFactory.createDataMasker(MaskValueType.SINGLE_VALUE.name(), maskConfig);
                column2Masker.put(columnName, masker);
            }
            maskConfigMap.put(TableIdentity.of(schemaName, tableName), column2Masker);
        }
        config.setMaskConfig(maskConfigMap);
    }

    private void clearWorkingDir() throws Exception {
        if (workingDir == null || !workingDir.exists()) {
            throw new FileNotFoundException("Working dir does not exist");
        }
        File dataPath = Paths.get(workingDir.getPath(), "data").toFile();

        if (dataPath.exists()) {
            boolean deleteRes = FileUtils.deleteQuietly(dataPath);
            log.info("Delete data directory, dir={}, result={}", dataPath.getAbsolutePath(), deleteRes);
        }
        for (File subFile : workingDir.listFiles()) {
            if (subFile.isDirectory()) {
                continue;
            }
            boolean deleteRes = FileUtils.deleteQuietly(subFile);
            log.info("Deleted file, fileName={}, result={}", subFile.getName(), deleteRes);
        }
    }

    private void handleOutput(DataTransferTaskResult result) throws Exception {
        if (!workingDir.exists()) {
            throw new FileNotFoundException(workingDir + " is not found");
        }
        File exportPath = Paths.get(workingDir.getPath(), "data").toFile();
        copyExportedFiles(result, exportPath.getPath());
        File dest = new File(workingDir.getPath() + File.separator + workingDir.getName() + "_export_file.zip");
        try {
            ExportOutput output = new ExportOutput(exportPath);
            output.toZip(dest, file -> !OUTPUT_FILTER_FILES.contains(file.getFileName()));
            if (config.isMergeSchemaFiles()) {
                File schemaFile =
                        new File(workingDir.getPath() + File.separator + workingDir.getName() + "_schema.sql");
                try {
                    ConnectType connectType = config.getConnectionInfo().getConnectType();
                    SchemaMergeOperator operator =
                            new SchemaMergeOperator(output, config.getSchemaName(), connectType.getDialectType());
                    operator.mergeSchemaFiles(schemaFile, filename -> !OUTPUT_FILTER_FILES.contains(filename));
                    // delete zip file if merge succeeded
                    FileUtils.deleteQuietly(dest);
                    dest = schemaFile;
                } catch (Exception ex) {
                    log.warn("merge schema failed, origin files will still be used, reason=", ex);
                }
            }
            result.setExportZipFilePath(dest.getName());
        } finally {
            boolean deleteRes = FileUtils.deleteQuietly(exportPath);
            log.info("Delete export directory, dir={}, result={}", exportPath.getAbsolutePath(), deleteRes);
        }
        adapter.afterHandle(config, result, dest);
    }

    private void validateSuccessful(DataTransferTaskResult result) {
        List<String> failedObjects = ListUtils.union(result.getDataObjectsInfo(), result.getSchemaObjectsInfo())
                .stream()
                .filter(objectStatus -> objectStatus.getStatus() != Status.SUCCESS)
                .map(ObjectResult::getSummary)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(failedObjects)) {
            LOGGER.warn("Data transfer task completed with unfinished objects! Details : {}", failedObjects);
        }
    }

    private List<URL> copyImportScripts(List<String> fileNames, DataTransferFormat format, File destDir)
            throws IOException {
        Validate.isTrue(CollectionUtils.isNotEmpty(fileNames), "No script found");
        Validate.notNull(format, "DataTransferFormat can not be null");
        if (DataTransferFormat.CSV.equals(format) && fileNames.size() > 1) {
            log.warn("Multiple files for CSV format is invalid, importFileNames={}", fileNames);
            throw new IllegalArgumentException("Multiple files isn't accepted for CSV format");
        }
        LocalFileManager fileManager = SpringContextUtil.getBean(LocalFileManager.class);
        List<URL> inputs = new ArrayList<>();
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
            inputs.add(dest.toURI().toURL());
        }
        return inputs;
    }

    private ExportOutput copyImportZip(List<String> fileNames, File destDir) throws IOException {
        if (fileNames == null || fileNames.size() != 1) {
            log.warn("Single zip file is available, importFileNames={}", fileNames);
            throw new IllegalArgumentException("Single zip file is available");
        }
        String fileName = fileNames.get(0);
        LocalFileManager fileManager = SpringContextUtil.getBean(LocalFileManager.class);
        Optional<File> uploadFile = fileManager.findByName(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET, fileName);
        File from = uploadFile.orElseThrow(() -> new FileNotFoundException("File not found, " + fileName));
        File dest = new File(destDir.getAbsolutePath() + File.separator + "data");
        FileUtils.forceMkdir(dest);
        ExportOutput exportOutput = new ExportOutput(from);
        exportOutput.toFolder(dest);
        log.info("Unzip file to working dir, from={}, dest={}", from.getAbsolutePath(), dest.getAbsolutePath());
        return new ExportOutput(dest);
    }

    private void copyExportedFiles(DataTransferTaskResult result, String exportPath) {
        doMove(result.getSchemaObjectsInfo(), exportPath);
        doMove(result.getDataObjectsInfo(), exportPath);
    }

    private void doMove(List<ObjectResult> objects, String exportPath) {
        if (CollectionUtils.isEmpty(objects)) {
            return;
        }
        for (ObjectResult object : objects) {
            if (CollectionUtils.isEmpty(object.getExportPaths())) {
                continue;
            }
            String objectType = object.getType();
            File objectDir = Paths.get(exportPath, objectType).toFile();
            try {
                if (!objectDir.exists()) {
                    FileUtils.forceMkdir(objectDir);
                }
                for (URL source : object.getExportPaths()) {
                    File file = new File(source.getFile());
                    if (!file.exists() || Objects.equals(file.getParentFile(), objectDir)) {
                        continue;
                    }
                    FileUtils.moveFileToDirectory(file, objectDir, false);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void replaceManifestIntoParameter(Manifest manifest) {
        CsvConfig csvConfig = new CsvConfig();
        config.setCsvConfig(csvConfig);
        if (Objects.isNull(manifest)) {
            LOGGER.warn("Transferring will continue with default csv config.");
            return;
        }
        csvConfig.setSkipHeader(manifest.isSkipHeader());
        csvConfig.setColumnSeparator(manifest.getColumnSeparator());
        csvConfig.setColumnDelimiter(manifest.getColumnDelimiter());
        csvConfig.setLineSeparator(manifest.getLineSeparator());
        csvConfig.setSkipHeader(manifest.isSkipHeader());
        csvConfig.setBlankToNull(StringUtils.equalsIgnoreCase(manifest.getNullString(), "null"));
    }

    private DumpParameter getDumpParameterForManifest() {
        CsvConfig csvConfig = MoreObjects.firstNonNull(config.getCsvConfig(), new CsvConfig());
        DumpParameter dumpParameter = new DumpParameter();
        dumpParameter.setFilePath(workingDir.getPath());
        dumpParameter.setColumnDelimiter(csvConfig.getColumnDelimiter());
        dumpParameter.setColumnSeparator(csvConfig.getColumnSeparator());
        dumpParameter.setLineSeparator(csvConfig.getLineSeparator());
        dumpParameter.setIgnoreEmptyLine(true);
        dumpParameter.setEscapeCharacter('\\');
        dumpParameter.setEmptyString("");
        dumpParameter.setDataFormat(DataFormat.CSV);
        if (csvConfig.isBlankToNull()) {
            dumpParameter.setNullString("null");
        }
        return dumpParameter;
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class TableColumn {
        private String tableName;
        private String columnName;
    }
}
