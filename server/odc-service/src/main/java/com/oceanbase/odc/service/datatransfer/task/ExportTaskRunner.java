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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.MaskValueType;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferTask;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.datatransfer.DataTransferAdapter;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.dumper.SchemaMergeOperator;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportTaskRunner extends BaseTransferTaskRunner {
    private static final Set<String> OUTPUT_FILTER_FILES = new HashSet<>();
    private final DataMaskingService maskingService;

    static {
        OUTPUT_FILTER_FILES.add(OdcConstants.PL_DEBUG_PACKAGE + "-schema.sql");
    }

    public ExportTaskRunner(DataTransferParameter parameter, Holder<DataTransferTask> jobHolder,
            DataTransferAdapter adapter, DataMaskingService maskingService) {
        super(parameter, jobHolder, adapter);
        this.maskingService = maskingService;
    }

    @Override
    protected void preHandle() throws Exception {
        if (adapter.getMaxDumpSizeBytes() != null) {
            parameter.setMaxDumpSizeBytes(adapter.getMaxDumpSizeBytes());
        }

        ConnectionSession connectionSession =
                new DefaultConnectSessionFactory(parameter.getConnectionConfig()).generateSession();
        String schemaName = parameter.getSchemaName();
        Map<String, List<String>> tableName2ColumnNames = new HashMap<>();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
            List<String> tableNames;
            if (parameter.isExportAllObjects()) {
                tableNames = accessor.showTablesLike(schemaName, null).stream()
                        .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                        .collect(Collectors.toList());
            } else {
                tableNames = parameter.getExportDbObjects().stream()
                        .filter(o -> Objects.equals(ObjectType.TABLE.getName(), o.getDbObjectType()))
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
                .listColumnsAndMaskingAlgorithm(parameter.getDatabaseId(), tableName2ColumnNames.keySet());
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
        parameter.setMaskConfig(maskConfigMap);
    }

    @Override
    protected void postHandle(DataTransferTaskResult result) throws Exception {
        File workingDir = parameter.getWorkingDir();
        if (!workingDir.exists()) {
            throw new FileNotFoundException(workingDir + " is not found");
        }

        File exportPath = Paths.get(workingDir.getPath(), "data").toFile();
        File dest = new File(workingDir.getPath() + File.separator + workingDir.getName() + "_export_file.zip");
        try {
            DumperOutput output = new DumperOutput(exportPath);
            output.toZip(dest, file -> !OUTPUT_FILTER_FILES.contains(file.getFileName()));
            if (parameter.isMergeSchemaFiles()) {
                File schemaFile =
                        new File(workingDir.getPath() + File.separator + workingDir.getName() + "_schema.sql");
                try {
                    ConnectType connectType = parameter.getConnectionInfo().getConnectType();
                    SchemaMergeOperator operator =
                            new SchemaMergeOperator(output, parameter.getSchemaName(), connectType.getDialectType());
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
        adapter.afterHandle(parameter, result, dest);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class TableColumn {
        private String tableName;
        private String columnName;
    }

}
