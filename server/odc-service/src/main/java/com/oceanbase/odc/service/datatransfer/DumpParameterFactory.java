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

import static com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants.MAX_BLOCK_SIZE_MEGABYTE;
import static com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants.MAX_CURSOR_FETCH_SIZE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.MaskValueType;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingFunction;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.function.context.ControlContext;
import com.oceanbase.tools.loaddump.function.context.ControlDescription;
import com.oceanbase.tools.loaddump.manager.ControlManager;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * {@link DumpParameterFactory} to generate {@link DumpParameter}
 *
 * @author yh263208
 * @date 2022-07-04 21:11
 * @since ODC_release_3.4.0
 * @see BaseParameterFactory
 */
public class DumpParameterFactory extends BaseParameterFactory<DumpParameter> {

    private final Long maxDumpSizeBytes;
    private final int fetchSize;
    private final DataMaskingService maskingService;

    public DumpParameterFactory(File workingDir, File logDir, ConnectionConfig connectionConfig,
            Long maxDumpSizeBytes, int fetchSize, DataMaskingService maskingService) throws FileNotFoundException {
        super(workingDir, logDir, connectionConfig);
        if (maxDumpSizeBytes != null) {
            Validate.isTrue(maxDumpSizeBytes > 0, "Max dump size can not be negative");
        }
        this.maxDumpSizeBytes = maxDumpSizeBytes;
        this.fetchSize = fetchSize;
        this.maskingService = maskingService;
    }

    @Override
    protected DumpParameter doGenerate(File workingDir, ConnectionConfig target,
            DataTransferConfig transferConfig) throws IOException {
        DumpParameter parameter = new DumpParameter();
        parameter.setSkipCheckDir(true);
        if (this.maxDumpSizeBytes != null) {
            parameter.setMaxFileSize(maxDumpSizeBytes);
        }
        parameter.setSchemaless(true);
        setFetchSize(parameter);
        setTransferFormat(parameter, transferConfig);
        setDumpObjects(parameter, target, transferConfig);
        if (transferConfig.isTransferDDL()) {
            parameter.setIncludeDdl(true);
            parameter.setDroppable(transferConfig.isWithDropDDL());
            parameter.setSnapshot(false);
        }
        if (transferConfig.isTransferData()) {
            if (transferConfig.getBatchCommitNum() != null) {
                parameter.setCommitSize(transferConfig.getBatchCommitNum());
            }
            parameter.setSnapshot(transferConfig.isGlobalSnapshot());
            if (transferConfig.getSkippedDataType() != null) {
                parameter.getExcludeDataTypes().addAll(transferConfig.getSkippedDataType());
            }
            if (maskingService.isMaskingEnabled()) {
                setMaskConfig(parameter, target, transferConfig);
            }
        }
        if (transferConfig.getDataTransferFormat() == DataTransferFormat.CSV) {
            parameter.setFileSuffix(".csv");
        }
        if (transferConfig.getDataTransferFormat() == DataTransferFormat.SQL) {
            parameter.setFileSuffix(".sql");
        }
        // The default limit in loader-dumper is 64MB. If there is no limit, set it to -1
        long exportFileMaxSize = transferConfig.getExportFileMaxSize();
        if (exportFileMaxSize <= 0) {
            parameter.setBlockSize(-1);
        } else if (exportFileMaxSize > MAX_BLOCK_SIZE_MEGABYTE) {
            throw new IllegalArgumentException(String.format("exportFileMaxSize %s MB has exceeded limit %s MB",
                    exportFileMaxSize, MAX_BLOCK_SIZE_MEGABYTE));
        } else {
            parameter.setBlockSize(BinarySizeUnit.MB.of(exportFileMaxSize).convert(BinarySizeUnit.B).getSizeDigit());
        }
        return parameter;
    }

    private void setDumpObjects(DumpParameter parameter, ConnectionConfig target, DataTransferConfig transferConfig)
            throws IOException {
        Map<ObjectType, Set<String>> whiteListMap = parameter.getWhiteListMap();
        if (whiteListMap == null) {
            throw new IllegalStateException("White list map is null");
        }
        if (transferConfig.isExportAllObjects()) {
            // 导出对象为空默认导出全部
            Map<ObjectType, Set<String>> allDumpObjects = getWhiteListMapForAll();
            Set<String> tableNames = getTableNames(target, transferConfig);
            if (tableNames.isEmpty()) {
                allDumpObjects.remove(ObjectType.TABLE);
            }
            if (transferConfig.isTransferDDL()) {
                whiteListMap.putAll(allDumpObjects);
            }
            if (transferConfig.isTransferData() && !tableNames.isEmpty()) {
                whiteListMap.put(ObjectType.TABLE, Collections.emptySet());
            }
        } else {
            List<DataTransferObject> objectList = transferConfig.getExportDbObjects();
            if (CollectionUtils.isEmpty(objectList)) {
                return;
            }
            if (transferConfig.isTransferDDL()) {
                whiteListMap.putAll(getWhiteListMap(objectList, o -> true));
            }
            if (transferConfig.isTransferData()) {
                whiteListMap.putAll(getWhiteListMap(objectList, o -> o.getDbObjectType() == ObjectType.TABLE));
            }
        }
    }

    /**
     * 导出全库设置，导入导出组件使用 {@link Collections#emptySet()} 代表导出该类型的全部对象
     */
    private Map<ObjectType, Set<String>> getWhiteListMapForAll() {
        Map<ObjectType, Set<String>> whiteListMap = new HashMap<>();
        whiteListMap.put(ObjectType.TABLE, Collections.emptySet());
        whiteListMap.put(ObjectType.TABLE_GROUP, Collections.emptySet());
        whiteListMap.put(ObjectType.VIEW, Collections.emptySet());
        whiteListMap.put(ObjectType.TRIGGER, Collections.emptySet());
        whiteListMap.put(ObjectType.SEQUENCE, Collections.emptySet());
        whiteListMap.put(ObjectType.SYNONYM, Collections.emptySet());
        whiteListMap.put(ObjectType.PUBLIC_SYNONYM, Collections.emptySet());
        whiteListMap.put(ObjectType.FUNCTION, Collections.emptySet());
        whiteListMap.put(ObjectType.PROCEDURE, Collections.emptySet());
        whiteListMap.put(ObjectType.TYPE, Collections.emptySet());
        whiteListMap.put(ObjectType.TYPE_BODY, Collections.emptySet());
        whiteListMap.put(ObjectType.PACKAGE, Collections.emptySet());
        whiteListMap.put(ObjectType.PACKAGE_BODY, Collections.emptySet());
        return whiteListMap;
    }

    private void setTransferFormat(DumpParameter parameter, DataTransferConfig transferConfig) {
        DataTransferFormat transferFormat = transferConfig.getDataTransferFormat();
        if (transferFormat == DataTransferFormat.SQL) {
            parameter.setDataFormat(DataFormat.SQL);
        } else if (transferFormat == DataTransferFormat.CSV) {
            parameter.setDataFormat(DataFormat.CSV);
        }
    }

    private Set<String> getTableNames(ConnectionConfig target, DataTransferConfig transferConfig) throws IOException {
        ConnectionSession session = new DefaultConnectSessionFactory(target).generateSession();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
            return accessor.showTables(transferConfig.getSchemaName()).stream()
                    .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            session.expire();
        }
    }

    private void setFetchSize(DumpParameter parameter) {
        /**
         * {@link java.sql.PreparedStatement#setFetchSize} 如果设置一个很大的值，cursor 会一次取很多数据放在缓存中，对内存消耗很大，所以我们将其限制在
         * {@value 1000} 以下。在桌面版这种 ODC 与 OBServer 网络延迟比较高的环境下，性能瓶颈为网络 I/O，因此默认设置为{@value 100}；而对于私有云/公有云这种
         * 延迟比较低的场景，导出性能的瓶颈并非是网络 I/O，用内存换性能的边际成本太低了，默认设置为 {@value 20} 即可.
         */
        parameter.setFetchSize(Math.min(fetchSize, MAX_CURSOR_FETCH_SIZE));
    }

    private void setMaskConfig(DumpParameter parameter, ConnectionConfig target, DataTransferConfig transferConfig) {
        ConnectionSession connectionSession = new DefaultConnectSessionFactory(target).generateSession();
        String schemaName = transferConfig.getSchemaName();
        Map<String, List<String>> tableName2ColumnNames = new HashMap<>();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
            List<String> tableNames;
            if (transferConfig.isExportAllObjects()) {
                tableNames = accessor.showTablesLike(schemaName, null).stream()
                        .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                        .collect(Collectors.toList());
            } else {
                tableNames = transferConfig.getExportDbObjects().stream()
                        .filter(o -> ObjectType.TABLE == o.getDbObjectType())
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
                .listColumnsAndMaskingAlgorithm(transferConfig.getDatabaseId(), tableName2ColumnNames.keySet());
        if (sensitiveColumn2Algorithm.isEmpty()) {
            return;
        }
        Map<TableColumn, MaskingAlgorithm> column2Algorithm = sensitiveColumn2Algorithm.keySet().stream()
                .collect(Collectors.toMap(c -> new TableColumn(c.getTableName(), c.getColumnName()),
                        sensitiveColumn2Algorithm::get, (c1, c2) -> c1));
        DataMaskerFactory maskerFactory = new DataMaskerFactory();
        ControlManager controlManager = ControlManager.newInstance();
        for (String tableName : tableName2ColumnNames.keySet()) {
            ControlContext controlContext = new ControlContext();
            for (String columnName : tableName2ColumnNames.get(tableName)) {
                MaskingAlgorithm algorithm = column2Algorithm.get(new TableColumn(tableName, columnName));
                if (Objects.isNull(algorithm)) {
                    continue;
                }
                ControlDescription controlDescription = new ControlDescription(columnName);
                MaskConfig maskConfig = MaskingAlgorithmUtil.toSingleFieldMaskConfig(algorithm, columnName);
                AbstractDataMasker masker =
                        maskerFactory.createDataMasker(MaskValueType.SINGLE_VALUE.name(), maskConfig);
                DataMaskingFunction function = new DataMaskingFunction(masker);
                controlDescription.add(function);
                controlContext.add(controlDescription);
            }
            controlManager.register(schemaName, tableName, controlContext);
        }
        parameter.setControlManager(controlManager);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class TableColumn {
        private String tableName;
        private String columnName;
    }

}
