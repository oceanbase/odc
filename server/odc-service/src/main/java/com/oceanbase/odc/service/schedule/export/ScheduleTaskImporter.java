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
package com.oceanbase.odc.service.schedule.export;

import static com.oceanbase.odc.service.exporter.model.ExportConstants.SCHEDULE_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.exporter.ImportService;
import com.oceanbase.odc.service.exporter.exception.ExtractFileException;
import com.oceanbase.odc.service.exporter.exception.InternalImportException;
import com.oceanbase.odc.service.exporter.impl.JsonExtractor;
import com.oceanbase.odc.service.exporter.model.ExportConstants;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataMapper;
import com.oceanbase.odc.service.exporter.model.ExportRowDataReader;
import com.oceanbase.odc.service.exporter.model.ImportResult;
import com.oceanbase.odc.service.exporter.utils.JsonExtractorFactory;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.InvalidFileFormatException;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.schedule.ScheduleExportFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.export.exception.DatabaseNonExistException;
import com.oceanbase.odc.service.schedule.export.model.BaseScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.DataArchiveScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.DataDeleteScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.ImportTaskResult;
import com.oceanbase.odc.service.schedule.export.model.PartitionPlanScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.ScheduleRowPreviewDto;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskImportRequest;
import com.oceanbase.odc.service.schedule.export.model.SqlPlanScheduleRowData;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ScheduleTaskImporter {

    @Autowired
    private BuildProperties buildProperties;

    @Autowired
    private ScheduleExportFacade scheduleExportFacade;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private ImportService importService;

    public List<ImportTaskResult> importSchedule(ScheduleTaskImportRequest request) {
        ScheduleType scheduleType = request.getScheduleType();
        try (JsonExtractor extractor =
                JsonExtractorFactory.buildJsonExtractor(objectStorageFacade, request.getBucketName(),
                        request.getObjectId(), request.getDecryptKey(), ".");
                ExportRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            checkMetaData(extractor, rowDataReader);
            checkScheduleType(rowDataReader, scheduleType);
            return doImportSchedule(scheduleType, request.getProjectId(), rowDataReader,
                    request.getImportableExportRowId());
        } catch (IOException e) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, e.getMessage(), e);
        }
    }

    private void checkScheduleType(ExportRowDataReader<JsonNode> rowDataReader, ScheduleType scheduleType) {
        ExportProperties properties = rowDataReader.getProperties();
        String exportScheduleType = properties.getStringValue(SCHEDULE_TYPE);
        if (!exportScheduleType.equals(scheduleType.name())) {
            throw new IllegalArgumentException("Incorrect schedule type");
        }
    }

    public List<ImportScheduleTaskView> preview(ScheduleTaskImportRequest request) {
        try (JsonExtractor extractor =
                JsonExtractorFactory.buildJsonExtractor(objectStorageFacade, request.getBucketName(),
                        request.getObjectId(), request.getDecryptKey(), ".");
                ExportRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            checkMetaData(extractor, rowDataReader);
            List<ScheduleRowPreviewDto> previewDto = getScheduleImportPreviewDto(rowDataReader);
            return scheduleExportFacade.preview(request.getScheduleType(), request.getProjectId(),
                    rowDataReader.getProperties(), previewDto);
        } catch (IOException e) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, e.getMessage(), e);
        }
    }

    private void doImportPartitionPlan(JsonNode row, Long projectId, Long databaseId)
            throws InternalImportException {
        PartitionPlanScheduleRowData currentRowData = JsonUtils.fromJsonNode(row, PartitionPlanScheduleRowData.class);
        if (currentRowData == null) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, "Can't extract partitionPlanScheduleRowData");
        }
        PartitionPlanConfig partitionPlanConfig = ExportRowDataMapper.INSTANCE.toPartitionPlanConfig(databaseId,
                currentRowData);
        CreateFlowInstanceReq createFlowInstanceReq = ExportRowDataMapper.INSTANCE.toCreateFlowInstanceReq(projectId,
                databaseId, TaskType.PARTITION_PLAN, partitionPlanConfig,
                currentRowData.getDescription());
        flowInstanceService.create(createFlowInstanceReq);
    }

    private List<ImportTaskResult> doImportSchedule(ScheduleType scheduleType, Long projectId,
            ExportRowDataReader<JsonNode> rowDataReader, Set<String> importableExportRowId)
            throws IOException {
        JsonNode currentRow = null;
        List<ImportTaskResult> results = new ArrayList<>();
        BaseScheduleRowData baseScheduleRowData = null;
        ExportProperties properties = rowDataReader.getProperties();
        while ((currentRow = rowDataReader.readRow()) != null) {
            try {
                baseScheduleRowData = JsonUtils.fromJsonNode(currentRow, BaseScheduleRowData.class);
                if (baseScheduleRowData == null) {
                    throw new ExtractFileException(ErrorCodes.ExtractFileFailed, "Can't extract rowData");
                }
                ImportResult importResult = importService.imported(properties, baseScheduleRowData.getRowId());
                if (importResult != ImportResult.NOT_IMPORTED) {
                    if (importResult == ImportResult.IMPORT_SUCCESS) {
                        log.info("Skip import, row id {} has been imported success", baseScheduleRowData.getRowId());
                    } else {
                        log.info("Skip import, row id {} has been imported failed", baseScheduleRowData.getRowId());
                    }
                    continue;
                }
                if (!importableExportRowId.contains(baseScheduleRowData.getRowId())) {
                    log.info("Row id {} is not in importableExportRowId", baseScheduleRowData.getRowId());
                    continue;
                }
                BaseScheduleRowData finalBaseScheduleRowData = baseScheduleRowData;
                JsonNode finalCurrentRow = currentRow;
                importService.importAndSaveHistory(properties, baseScheduleRowData.getRowId(),
                        () -> doImport(scheduleType, projectId, finalBaseScheduleRowData, finalCurrentRow, results));
            } catch (RuntimeException e) {
                String rowId = Optional.ofNullable(baseScheduleRowData).map(BaseScheduleRowData::getRowId).orElse(null);
                results.add(ImportTaskResult.failed(rowId, e.getMessage()));
            }
        }
        return results;
    }

    private boolean doImport(ScheduleType scheduleType, Long projectId, BaseScheduleRowData baseScheduleRowData,
            JsonNode currentRow, List<ImportTaskResult> results) {
        Long databaseId = null;
        Long targetDatabaseId = null;
        try {
            databaseId = scheduleExportFacade.getOrCreateDatabaseId(projectId, baseScheduleRowData.getDatabase());
            if (baseScheduleRowData.getTargetDatabase() != null) {
                targetDatabaseId = scheduleExportFacade.getOrCreateDatabaseId(projectId,
                        baseScheduleRowData.getTargetDatabase());
            }
        } catch (DatabaseNonExistException e) {
            results.add(ImportTaskResult.failed(baseScheduleRowData.getRowId(), "Database not exist"));
            return false;
        }

        if (scheduleType == ScheduleType.PARTITION_PLAN) {
            // The partition plan creates flow first
            doImportPartitionPlan(currentRow, projectId, databaseId);
            return true;
        }
        CreateFlowInstanceReq createScheduleReq =
                getCreateScheduleReq(scheduleType, projectId, currentRow, databaseId,
                        targetDatabaseId);
        scheduleService.dispatchCreateSchedule(createScheduleReq);
        results.add(ImportTaskResult.success(baseScheduleRowData.getRowId()));
        return true;
    }

    private CreateFlowInstanceReq getCreateScheduleReq(ScheduleType scheduleType, Long projectId, JsonNode row,
            Long databaseId,
            Long targetDatabaseId) {
        switch (scheduleType) {
            case SQL_PLAN:
                return getSqlPlanReq(row, projectId, databaseId);
            case DATA_DELETE:
                return getDataDeleteReq(row, projectId, databaseId, targetDatabaseId);
            case DATA_ARCHIVE:
                return getDataArchiveReq(row, projectId, databaseId, targetDatabaseId);
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
    }

    private CreateFlowInstanceReq getSqlPlanReq(JsonNode row, Long projectId, Long databaseId) {
        SqlPlanScheduleRowData currentRowData = JsonUtils.fromJsonNode(row, SqlPlanScheduleRowData.class);
        if (currentRowData == null) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, "Can't extract sqlPlanScheduleRowData");
        }
        SqlPlanParameters sqlPlanParameters = ExportRowDataMapper.INSTANCE.toSqlPlanParameters(databaseId,
                currentRowData);

        AlterScheduleParameters alterScheduleParameters = ExportRowDataMapper.INSTANCE.toAlterScheduleParameters(
                OperationType.CREATE, currentRowData, sqlPlanParameters);
        return ExportRowDataMapper.INSTANCE.toCreateFlowInstanceReq(projectId,
                databaseId, TaskType.ALTER_SCHEDULE, alterScheduleParameters,
                currentRowData.getDescription());
    }

    private CreateFlowInstanceReq getDataDeleteReq(JsonNode row, Long projectId, Long databaseId,
            Long targetDatabaseId) {
        DataDeleteScheduleRowData currentRowData = JsonUtils.fromJsonNode(row, DataDeleteScheduleRowData.class);
        if (currentRowData == null) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, "Can't extract sqlPlanScheduleRowData");
        }
        DataDeleteParameters dataDeleteParameters =
                ExportRowDataMapper.INSTANCE.toDataDeleteParameters(databaseId, targetDatabaseId,
                        currentRowData);
        AlterScheduleParameters alterScheduleParameters = ExportRowDataMapper.INSTANCE
                .toAlterScheduleParameters(OperationType.CREATE, currentRowData, dataDeleteParameters);
        return ExportRowDataMapper.INSTANCE.toCreateFlowInstanceReq(projectId,
                databaseId, TaskType.ALTER_SCHEDULE, alterScheduleParameters,
                currentRowData.getDescription());
    }


    private CreateFlowInstanceReq getDataArchiveReq(JsonNode row, Long projectId, Long databaseId,
            Long targetDatabaseId) {
        DataArchiveScheduleRowData currentRowData = JsonUtils.fromJsonNode(row, DataArchiveScheduleRowData.class);
        if (currentRowData == null) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, "Can't extract sqlPlanScheduleRowData");
        }
        DataArchiveParameters dataArchiveParameters =
                ExportRowDataMapper.INSTANCE.toDataArchiveParameters(databaseId, targetDatabaseId,
                        currentRowData);
        AlterScheduleParameters alterScheduleParameters = ExportRowDataMapper.INSTANCE
                .toAlterScheduleParameters(OperationType.CREATE, currentRowData, dataArchiveParameters);
        return ExportRowDataMapper.INSTANCE.toCreateFlowInstanceReq(projectId,
                databaseId, TaskType.ALTER_SCHEDULE, alterScheduleParameters,
                currentRowData.getDescription());
    }

    private List<ScheduleRowPreviewDto> getScheduleImportPreviewDto(ExportRowDataReader<JsonNode> rowDataReader) {
        List<ScheduleRowPreviewDto> preview = new ArrayList<>();
        BaseScheduleRowData baseScheduleRowData;
        try {
            while ((baseScheduleRowData = rowDataReader.readRow(BaseScheduleRowData.class)) != null) {
                preview.add(baseScheduleRowData.preview());
            }
            return preview;
        } catch (IOException e) {
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, e.getMessage(), e);
        }
    }

    private void checkMetaData(JsonExtractor extractor, ExportRowDataReader<JsonNode> rowDataReader) {
        if (!extractor.checkSignature()) {
            throw new ExtractFileException(ErrorCodes.InvalidSignature, "Invalid signature");
        }

        ExportProperties metaData = rowDataReader.getProperties();
        String exportType = metaData.acquireExportType();
        if (exportType == null || !exportType.equals(ExportConstants.SCHEDULE_ARCHIVE_TYPE)) {
            throw new InvalidFileFormatException(ErrorCodes.IllegalFileName, "Export Type is not match");
        }
        // Cross-version import and export is not supported at present
        String odcVersion = metaData.acquireOdcVersion();
        if (!buildProperties.getVersion().equals(odcVersion)) {
            log.error("Expected build properties version {} but found {}", buildProperties.getVersion(), odcVersion);
            throw new ExtractFileException(ErrorCodes.ExtractFileFailed, "ODC version is not match");
        }
    }
}
