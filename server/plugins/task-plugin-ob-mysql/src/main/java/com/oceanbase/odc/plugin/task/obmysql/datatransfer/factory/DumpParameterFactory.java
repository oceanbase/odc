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

package com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory;

import static com.oceanbase.odc.core.shared.constant.OdcConstants.VALIDATE_DDL_TABLE_POSTFIX;
import static com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants.MAX_BLOCK_SIZE_MEGABYTE;
import static com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants.MAX_CURSOR_FETCH_SIZE;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.export.DataMaskingFunction;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.ConnectionUtil;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.PluginUtil;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.function.context.ControlContext;
import com.oceanbase.tools.loaddump.function.context.ControlDescription;
import com.oceanbase.tools.loaddump.manager.ControlManager;

/**
 * {@link DumpParameterFactory} to generate {@link DumpParameter}
 *
 * @author yh263208
 * @date 2022-07-04 21:11
 * @since ODC_release_3.4.0
 * @see BaseParameterFactory
 */
public class DumpParameterFactory extends BaseParameterFactory<DumpParameter> {

    public DumpParameterFactory(DataTransferConfig config, File workingDir, File logDir) {
        super(config, workingDir, logDir);
        if (config.getMaxDumpSizeBytes() != null) {
            Validate.isTrue(config.getMaxDumpSizeBytes() > 0, "Max dump size can not be negative");
        }
    }

    @Override
    protected DumpParameter doGenerate(File workingDir) throws IOException {
        DumpParameter parameter = new DumpParameter();
        if (StringUtils.isNotEmpty(transferConfig.getQuerySql())) {
            parameter.setQuerySql(transferConfig.getQuerySql());
        }
        parameter.setSkipCheckDir(true);
        if (transferConfig.getMaxDumpSizeBytes() != null) {
            parameter.setMaxFileSize(transferConfig.getMaxDumpSizeBytes());
        }
        parameter.setSchemaless(true);
        setFetchSize(parameter);
        setTransferFormat(parameter, transferConfig);
        setDumpObjects(parameter, transferConfig);
        if (transferConfig.isTransferDDL()) {
            parameter.setIncludeDdl(true);
            parameter.setDroppable(transferConfig.isWithDropDDL());
            parameter.setSnapshot(false);
        }
        if (transferConfig.isTransferData()) {
            parameter.setPageSize(Integer.MAX_VALUE);
            parameter.setRetainEmptyFiles(true);
            if (transferConfig.getBatchCommitNum() != null) {
                parameter.setCommitSize(transferConfig.getBatchCommitNum());
            }
            parameter.setSnapshot(transferConfig.isGlobalSnapshot());
            if (transferConfig.getSkippedDataType() != null) {
                parameter.getExcludeDataTypes().addAll(transferConfig.getSkippedDataType());
            }
            if (MapUtils.isNotEmpty(transferConfig.getMaskConfig())) {
                setMaskConfig(parameter, transferConfig);
            }
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

    private void setDumpObjects(DumpParameter parameter, DataTransferConfig transferConfig) {
        Map<ObjectType, Set<String>> whiteListMap = parameter.getWhiteListMap();
        if (whiteListMap == null) {
            throw new IllegalStateException("White list map is null");
        }
        if (transferConfig.isExportAllObjects()) {
            // 导出对象为空默认导出全部
            Map<ObjectType, Set<String>> allDumpObjects = getWhiteListMapForAll();
            Set<String> tableNames = getTableNames(transferConfig);
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
        switch (transferFormat) {
            case SQL:
                parameter.setDataFormat(DataFormat.SQL);
                parameter.setFileSuffix(DataTransferFormat.SQL.getExtension());
                return;
            case CSV:
            case EXCEL:
                parameter.setDataFormat(DataFormat.CSV);
                parameter.setFileSuffix(DataTransferFormat.CSV.getExtension());
                return;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Set<String> getTableNames(DataTransferConfig transferConfig) {
        try (SingleConnectionDataSource dataSource =
                ConnectionUtil.getDataSource(transferConfig.getConnectionInfo(), transferConfig.getSchemaName());
                Connection connection = dataSource.getConnection()) {
            return PluginUtil.getTableExtension(transferConfig.getConnectionInfo())
                    .showNamesLike(connection, transferConfig.getSchemaName(), "").stream()
                    .filter(table -> !StringUtils.endsWithIgnoreCase(table, VALIDATE_DDL_TABLE_POSTFIX))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new UnexpectedException("Failed to get export table names.", e);
        }
    }

    private void setFetchSize(DumpParameter parameter) {
        /**
         * {@link java.sql.PreparedStatement#setFetchSize} 如果设置一个很大的值，cursor 会一次取很多数据放在缓存中，对内存消耗很大，所以我们将其限制在
         * {@value 1000} 以下。在桌面版这种 ODC 与 OBServer 网络延迟比较高的环境下，性能瓶颈为网络 I/O，因此默认设置为{@value 100}；而对于私有云/公有云这种
         * 延迟比较低的场景，导出性能的瓶颈并非是网络 I/O，用内存换性能的边际成本太低了，默认设置为 {@value 20} 即可.
         */
        parameter.setFetchSize(Math.min(transferConfig.getCursorFetchSize(), MAX_CURSOR_FETCH_SIZE));
    }

    private void setMaskConfig(DumpParameter parameter, DataTransferConfig transferConfig) {
        Map<TableIdentity, Map<String, AbstractDataMasker>> maskConfigMap = transferConfig.getMaskConfig();
        ControlManager controlManager = ControlManager.newInstance();
        for (Entry<TableIdentity, Map<String, AbstractDataMasker>> entry : maskConfigMap.entrySet()) {
            ControlContext controlContext = new ControlContext();
            for (Entry<String, AbstractDataMasker> column2Masker : entry.getValue().entrySet()) {
                ControlDescription controlDescription = new ControlDescription(column2Masker.getKey());
                DataMaskingFunction function = new DataMaskingFunction(column2Masker.getValue());
                controlDescription.add(function);
                controlContext.add(controlDescription);
            }
            controlManager.register(entry.getKey().getSchemaName(), entry.getKey().getTableName(), controlContext);
        }
        parameter.setControlManager(controlManager);
        parameter.setUseRuntimeTableName(true);
    }

}

