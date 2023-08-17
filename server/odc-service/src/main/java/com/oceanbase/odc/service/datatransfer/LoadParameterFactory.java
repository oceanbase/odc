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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datatransfer.dumper.AbstractOutputFile;
import com.oceanbase.odc.service.datatransfer.dumper.BinaryFile;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.datatransfer.model.DataTransferObject;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.common.model.Manifest;
import com.oceanbase.tools.loaddump.common.model.MapObject;
import com.oceanbase.tools.loaddump.function.context.ControlContext;
import com.oceanbase.tools.loaddump.manager.ControlManager;

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
    public LoadParameterFactory(File workingDir, File logDir, ConnectionConfig connectionConfig)
            throws FileNotFoundException {
        super(workingDir, logDir, connectionConfig);
    }

    @Override
    protected LoadParameter doGenerate(File workingDir, ConnectionConfig target,
            DataTransferConfig config) throws IOException {
        LoadParameter parameter = new LoadParameter();
        parameter.setMaxErrors(config.isStopWhenError() ? 0 : -1);
        setTransferFormat(parameter, config);
        if (!config.isNotObLoaderDumperCompatible()) {
            /**
             * 导入导出组件产出物导入，需要详细设置
             */
            setWhiteListForZip(parameter, config, workingDir, target);
            if (config.isTransferDDL()) {
                parameter.setIncludeDdl(true);
                parameter.setReplaceObjectIfExists(config.isReplaceSchemaWhenExists());
            }
            if (config.isTransferData()) {
                parameter.setTruncatable(config.isTruncateTableBeforeImport());
                if (config.getBatchCommitNum() != null) {
                    parameter.setBatchSize(config.getBatchCommitNum());
                }
                if (config.getSkippedDataType() != null) {
                    parameter.getExcludeDataTypes().addAll(config.getSkippedDataType());
                }
            }
        } else if (isExternalCsv(config)) {
            // 单表导入 csv 文件场景
            parameter.setTruncatable(config.isTruncateTableBeforeImport());
            setCsvMappings(parameter, config);
            setWhiteListForExternalCsv(parameter, config, workingDir);
            parameter.setFileSuffix(DataFormat.CSV.getDefaultFileSuffix());
        } else if (config.getDataTransferFormat() == DataTransferFormat.SQL) {
            parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
        }
        return parameter;
    }

    private void setWhiteListForZip(LoadParameter parameter, DataTransferConfig config,
            File workingDir, ConnectionConfig target) throws IOException {
        if (config.isNotObLoaderDumperCompatible()) {
            return;
        }
        DumperOutput dumperOutput = new DumperOutput(new File(workingDir.getAbsolutePath() + File.separator + "data"));
        List<AbstractOutputFile> outputFiles = dumperOutput.getAllDumpFiles();
        Validate.isTrue(!outputFiles.isEmpty(), "No import object found");
        Validate.isTrue(!config.isTransferData() || dumperOutput.isContainsData(), "Input does not contain data");
        Validate.isTrue(!config.isTransferDDL() || dumperOutput.isContainsSchema(), "Input does not contain schema");
        Map<ObjectType, Set<String>> whiteList = parameter.getWhiteListMap();
        if (whiteList == null) {
            throw new IllegalStateException("White list map is null");
        }
        List<DataTransferObject> objectList = config.getExportDbObjects();
        if (CollectionUtils.isEmpty(objectList)) {
            for (AbstractOutputFile outputFile : outputFiles) {
                Set<String> names = whiteList.computeIfAbsent(outputFile.getObjectType(), t -> new HashSet<>());
                if (DialectType.OB_ORACLE.equals(target.getDialectType())) {
                    names.add(StringUtils.quoteOracleIdentifier(outputFile.getObjectName()));
                } else {
                    names.add(StringUtils.quoteMysqlIdentifier(outputFile.getObjectName()));
                }
            }
        } else {
            whiteList.putAll(getWhiteListMap(objectList, o -> true));
        }
        BinaryFile<Manifest> manifest = dumperOutput.getManifest();
        /**
         * only CSV format would save the manifest {@link com.oceanbase.tools.loaddump.client.DumpClient}
         */
        if (manifest != null) {
            parameter.setFileSuffix(DataFormat.CSV.getDefaultFileSuffix());
        } else {
            parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
        }

    }

    private void setTransferFormat(LoadParameter parameter, DataTransferConfig transferConfig) {
        if (!transferConfig.isTransferData()) {
            return;
        }
        DataTransferFormat format = transferConfig.getDataTransferFormat();
        if (DataTransferFormat.SQL.equals(format)) {
            if (transferConfig.isNotObLoaderDumperCompatible()) {
                parameter.setDataFormat(DataFormat.MIX);
                parameter.setFileSuffix(DataFormat.MIX.getDefaultFileSuffix());
            } else {
                parameter.setDataFormat(DataFormat.SQL);
            }
            parameter.setExternal(transferConfig.isNotObLoaderDumperCompatible());
        } else if (DataTransferFormat.CSV.equals(format)) {
            parameter.setDataFormat(DataFormat.CSV);
            parameter.setExternal(transferConfig.isNotObLoaderDumperCompatible());
        }
    }

    private boolean isExternalCsv(DataTransferConfig config) {
        return DataTransferFormat.CSV == config.getDataTransferFormat() && config.isNotObLoaderDumperCompatible();
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
        ControlManager controlManager = ControlManager.newInstance();
        controlManager.register(transferConfig.getSchemaName(), tableName, new ControlContext());
        parameter.setControlManager(controlManager);
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
        parameter.getWhiteListMap().putAll(getWhiteListMap(objectList, o -> o.getDbObjectType() == ObjectType.TABLE));
        if (parameter.getWhiteListMap().size() != 0) {
            return;
        }
        throw new IllegalArgumentException("No effective white list map settings");
    }

}
