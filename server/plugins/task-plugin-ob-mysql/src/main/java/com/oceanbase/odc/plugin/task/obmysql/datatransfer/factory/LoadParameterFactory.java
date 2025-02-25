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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.common.model.MapObject;
import com.oceanbase.tools.loaddump.utils.SerializeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link LoadParameterFactory} to generate {@link LoadParameter}
 *
 * @author yh263208
 * @date 2022-06-29 17:02
 * @see BaseParameterFactory
 * @since ODC_release_3.4.0
 */
@Slf4j
public class LoadParameterFactory extends BaseParameterFactory<LoadParameter> {
    private static final Integer DEFAULT_INSERT_BATCH_SIZE = 100;

    public LoadParameterFactory(DataTransferConfig config, File workingDir, File logDir) {
        super(config, workingDir, logDir);
    }

    @Override
    protected LoadParameter doGenerate(File workingDir) throws IOException {
        LoadParameter parameter = new LoadParameter();
        if (transferConfig.isStopWhenError()) {
            parameter.setMaxErrors(0);
            parameter.setMaxDiscards(0);
            /*
             * set it to TRUE indicates the process should exit with failure if there is any bad or discard
             * record.
             */
            parameter.setStrict(true);
        } else {
            parameter.setMaxErrors(-1);
            parameter.setMaxDiscards(-1);
        }
        setTransferFormat(parameter, transferConfig);
        if (transferConfig.isZipOrDir()) {
            /**
             * 导入导出组件产出物导入，需要详细设置
             */
            setWhiteListForZipOrDir(parameter, transferConfig);
            if (transferConfig.isTransferDDL()) {
                parameter.setIncludeDdl(true);
                parameter.setReplaceObjectIfExists(transferConfig.isReplaceSchemaWhenExists());
            }
            if (transferConfig.isTransferData()) {
                parameter.setTruncatable(transferConfig.isTruncateTableBeforeImport());
                parameter.setBatchSize(
                        MoreObjects.firstNonNull(transferConfig.getBatchCommitNum(), DEFAULT_INSERT_BATCH_SIZE));
                if (transferConfig.getSkippedDataType() != null) {
                    parameter.getExcludeDataTypes().addAll(transferConfig.getSkippedDataType());
                }
            }
        } else if (isExternalCsv(transferConfig)) {
            // 单表导入 csv 文件场景
            parameter.setTruncatable(transferConfig.isTruncateTableBeforeImport());
            setCsvMappings(parameter, transferConfig);
            setWhiteListForExternalCsv(parameter, transferConfig, workingDir);
            parameter.setBatchSize(
                    MoreObjects.firstNonNull(transferConfig.getBatchCommitNum(), DEFAULT_INSERT_BATCH_SIZE));
            if (transferConfig.getSkippedDataType() != null) {
                parameter.getExcludeDataTypes().addAll(transferConfig.getSkippedDataType());
            }
        } else if (transferConfig.getDataTransferFormat() == DataTransferFormat.SQL) {
            parameter.setReplaceObjectIfExists(true);
        }
        return parameter;
    }

    private void setWhiteListForZipOrDir(LoadParameter parameter, DataTransferConfig config) throws IOException {
        if (!config.isZipOrDir()) {
            return;
        }
        Map<ObjectType, Set<String>> whiteList = parameter.getWhiteListMap();
        if (whiteList == null) {
            throw new IllegalStateException("White list map is null");
        }

        List<DataTransferObject> objectList = config.getExportDbObjects();
        if (CollectionUtils.isEmpty(objectList)) {
            throw new IllegalArgumentException("No object was found for white list.");
        }
        whiteList.putAll(getWhiteListMap(objectList, o -> true));

        File manifest = Paths.get(workingDir.getPath(), "MANIFEST.BIN").toFile();
        /**
         * only CSV format would save the manifest {@link com.oceanbase.tools.loaddump.client.DumpClient}
         */
        if (manifest.exists() && manifest.isFile()) {
            try {
                if (SerializeUtils.deserializeObjectByKryo(manifest.getPath()) == null) {
                    log.warn("Failed to deserialize MANIFEST.BIN, please check if different versions of ODC or "
                            + "ob-loader-dumper were used between export and import.");
                }
            } catch (Exception e) {
                log.warn("Failed to deserialize MANIFEST.BIN, please check if different versions of ODC or "
                        + "ob-loader-dumper were used between export and import.");
            }
        }

    }

    private void setTransferFormat(LoadParameter parameter, DataTransferConfig transferConfig) {
        if (!transferConfig.isTransferData()) {
            parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
            return;
        }
        DataTransferFormat format = transferConfig.getDataTransferFormat();
        if (DataTransferFormat.SQL.equals(format)) {
            boolean isZipOrDirImport = transferConfig.isZipOrDir();
            if (isZipOrDirImport) {
                parameter.setDataFormat(DataFormat.SQL);
                parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
            } else {
                parameter.setDataFormat(DataFormat.MIX);
                parameter.setFileSuffix(DataFormat.MIX.getDefaultFileSuffix());
            }
            parameter.setExternal(!isZipOrDirImport);
        } else if (DataTransferFormat.CSV.equals(format)) {
            parameter.setDataFormat(DataFormat.CSV);
            parameter.setFileSuffix(DataFormat.CSV.getDefaultFileSuffix());
            parameter.setExternal(!transferConfig.isZipOrDir());
        }
    }

    private boolean isExternalCsv(DataTransferConfig config) {
        return DataTransferFormat.CSV == config.getDataTransferFormat() && !transferConfig.isZipOrDir();
    }

    private void setCsvMappings(LoadParameter parameter, DataTransferConfig transferConfig) {
        if (!isExternalCsv(transferConfig)) {
            return;
        }
        List<CsvColumnMapping> mappings = transferConfig.getCsvColumnMappings();
        List<DataTransferObject> dbObjects = transferConfig.getExportDbObjects();
        if (mappings == null || CollectionUtils.isEmpty(dbObjects)) {
            return;
        }
        Map<String, MapObject> csvMapping = new LinkedHashMap<>();
        for (CsvColumnMapping mapping : mappings) {
            // target 代表该列在数据库表中的位置
            Integer target = mapping.getDestColumnPosition();
            // source 代表该列在数据文件中的位置
            Integer source = mapping.getSrcColumnPosition();
            if (target == null || source == null) {
                continue;
            }
            csvMapping.put(mapping.getDestColumnName(), new MapObject(source, target - 1));
        }
        String tableName = dbObjects.get(0).getObjectName();
        parameter.getColumnNameMapping().put(tableName, csvMapping);
    }

    /**
     * notice:
     *
     * <pre>
     *     External sql format do not need any export information, and it will use schema load.
     *     External csv format need TABLE export information. SQL format do not need any information.
     *     CSV format do not need any information.
     * </pre>
     */
    private void setWhiteListForExternalCsv(LoadParameter parameter, DataTransferConfig config,
            File workingDir) {
        if (!isExternalCsv(config)) {
            return;
        }
        List<File> csvFiles = Arrays.stream(workingDir.listFiles())
                .filter(file -> file.isFile() && (StringUtils.endsWithIgnoreCase(file.getName(), "csv")
                        || StringUtils.endsWithIgnoreCase(file.getName(), "txt")))
                .collect(Collectors.toList());
        Validate.isTrue(!csvFiles.isEmpty(), "Input csv file is not found");
        Validate.isTrue(csvFiles.size() == 1, "Multiple csv files are not supported");
        File csvFile = csvFiles.get(0);
        if (!csvFile.exists()) {
            throw new IllegalStateException("Input csv file does not exist");
        }
        parameter.setInputFile(csvFile);
        List<DataTransferObject> objectList = config.getExportDbObjects();
        Validate.isTrue(CollectionUtils.isNotEmpty(objectList), "Import objects is necessary");
        parameter.getWhiteListMap().putAll(
                getWhiteListMap(objectList, o -> o.getDbObjectType() == ObjectType.TABLE));
        if (parameter.getWhiteListMap().size() != 0) {
            return;
        }
        throw new IllegalArgumentException("No effective white list map settings");
    }

}
