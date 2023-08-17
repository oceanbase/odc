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
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.service.datatransfer.model.CsvConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.service.datatransfer.model.DataTransferType;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.common.model.MapObject;

/**
 * {@link LoadParameterFactoryTest}
 *
 * @author yh263208
 * @date 2022-07-27 14:47
 * @since ODC_release_3.4.0
 */
public class LoadParameterFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void generate_stopWhenErrorSetting_return1() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig());
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, false, true, true);
        config.setStopWhenError(true);
        LoadParameter parameter = factory.generate(config);
        Assert.assertEquals(0, parameter.getMaxErrors());
    }

    @Test
    public void generate_doNotStopWhenErrorSetting_returnMaxInteger() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig());
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, false, true, true);
        config.setStopWhenError(false);
        LoadParameter parameter = factory.generate(config);
        Assert.assertEquals(-1, parameter.getMaxErrors());
    }

    @Test
    public void generate_externalSqlFormat_returnMix() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig());
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.SQL, true, true, false));
        Assert.assertEquals(DataFormat.MIX, parameter.getDataFormat());
    }

    @Test
    public void generate_sqlFormat_returnSql() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig());
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.SQL, false, true, false));
        Assert.assertEquals(DataFormat.SQL, parameter.getDataFormat());
    }

    @Test
    public void generate_csvFormat_returnCsv() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig());
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, false, true, false));
        Assert.assertEquals(DataFormat.CSV, parameter.getDataFormat());
    }

    @Test
    public void generate_nonExternalNonExportDbObjectsMysqlWhiteListMap_returnTESTTable() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_MYSQL));
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, false, true);
        config.setExportDbObjects(null);
        LoadParameter parameter = factory.generate(config);
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("`TEST`"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nonExternalNonExportDbObjectsOracleWhiteListMap_returnTESTTable() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, false, true);
        config.setExportDbObjects(null);
        LoadParameter parameter = factory.generate(config);
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("\"TEST\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nonExternalMysqlWhiteListMap_returnTESTTable() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_MYSQL));
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, false, true);
        LoadParameter parameter = factory.generate(config);
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        for (DataTransferObject object : config.getExportDbObjects()) {
            Set<String> names = expect.computeIfAbsent(object.getDbObjectType(), k -> new HashSet<>());
            names.add(StringUtils.quoteMysqlIdentifier(object.getObjectName()));
        }
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nonExternalOracleWhiteListMap_returnTESTTable() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, false, true);
        LoadParameter parameter = factory.generate(config);
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        for (DataTransferObject object : config.getExportDbObjects()) {
            Set<String> names = expect.computeIfAbsent(object.getDbObjectType(), k -> new HashSet<>());
            names.add(StringUtils.quoteOracleIdentifier(object.getObjectName()));
        }
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nonExternalIncludeDdl_returnTrue() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.SQL, false, false, true));
        Assert.assertTrue(parameter.isIncludeDdl());
    }

    @Test
    public void generate_externalCsvCsvMappingConfig_returnTrue() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false);
        LoadParameter parameter = factory.generate(config);
        Map<String, Map<String, MapObject>> actual = parameter.getColumnNameMapping();

        Map<String, Map<String, MapObject>> expect = new HashMap<>();
        Map<String, MapObject> csvMapping = new HashMap<>();
        for (CsvColumnMapping mapping : config.getCsvColumnMappings()) {
            Integer source = mapping.getSrcColumnPosition();
            Integer target = mapping.getDestColumnPosition();
            csvMapping.put(mapping.getDestColumnName(), new MapObject(source, target - 1));
        }
        expect.put("TAB", csvMapping);

        Assert.assertEquals(expect.get("TAB").get("COL2").getSource(), actual.get("TAB").get("COL2").getSource());
        Assert.assertEquals(expect.get("TAB").get("COL2").getTarget(), actual.get("TAB").get("COL2").getTarget());
    }

    @Test
    public void generate_externalCsvWhiteListMap_returnTABTABLE() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, true, true, false));
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("\"TAB\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_externalCsvInvalidObjectList_expThrown() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("TAB");
        object.setDbObjectType(ObjectType.VIEW);
        config.setExportDbObjects(Collections.singletonList(object));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No effective white list map settings");
        factory.generate(config);
    }

    @Test
    public void generate_isNoSys_returnTrue() throws IOException {
        ConnectionConfig connectionConfig = getConnectionConfig(DialectType.OB_ORACLE);
        connectionConfig.setSysTenantPassword(null);
        connectionConfig.setSysTenantUsername(null);
        LoadParameterFactory factory = new LoadParameterFactory(getWorkingDir(), getWorkingDir(), connectionConfig);
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, true, true, false));
        Assert.assertTrue(parameter.isNoSys());
    }

    @Test
    public void generate_setCsvInfoDoubleExcapeN_returnExpectedCsvInfo() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false);
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setLineSeparator("\\n");
        config.setCsvConfig(csvConfig);
        LoadParameter parameter = factory.generate(config);
        Assert.assertEquals("\n", parameter.getLineSeparator());
    }

    @Test
    public void generate_setCsvInfoExcapeN_returnExpectedCsvInfo() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, true, true, false));
        Assert.assertEquals("\n", parameter.getLineSeparator());
    }

    @Test
    public void generate_setCsvInfoDoubleExcapeR_returnExpectedCsvInfo() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false);
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setLineSeparator("\\r");
        config.setCsvConfig(csvConfig);
        LoadParameter parameter = factory.generate(config);
        Assert.assertEquals("\r", parameter.getLineSeparator());
    }

    @Test
    public void generate_blankToNull_returnNullEmptyReplacer() throws IOException {
        LoadParameterFactory factory =
                new LoadParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false);
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setBlankToNull(true);
        config.setCsvConfig(csvConfig);
        LoadParameter parameter = factory.generate(config);
        Assert.assertNull(parameter.getEmptyReplacer());
        Assert.assertEquals("null", parameter.getNullString());
    }

    @Test
    public void generate_workingDirAssignedToFilePath_pathEquals() throws IOException {
        File workingDir = getWorkingDir();
        LoadParameterFactory factory =
                new LoadParameterFactory(workingDir, getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        LoadParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, true, true, false));
        Assert.assertEquals(workingDir.getAbsolutePath(), parameter.getFilePath());
    }

    @Test
    public void generate_fileEncoding_encodingEquals() throws IOException {
        File workingDir = getWorkingDir();
        LoadParameterFactory factory =
                new LoadParameterFactory(workingDir, getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false);
        LoadParameter parameter = factory.generate(config);
        Assert.assertEquals(config.getEncoding().getAlias(), parameter.getFileEncoding());
    }

    @Test
    public void baseParameterFactory_nonExistsWorkingDir_expThrown() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("Working dir does not exist");
        new LoadParameterFactory(new File("/a/b/c"), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
    }

    @Test
    public void baseParameterFactory_workingDirIsFile_expThrown() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Working dir is not a dir");
        new LoadParameterFactory(getDumpZip(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE));
    }

    private DataTransferConfig generateConfig(DataTransferFormat format, boolean external,
            boolean transferData, boolean transferDdl) {
        DataTransferConfig config = new DataTransferConfig();
        config.setSchemaName("test");
        config.setTransferType(DataTransferType.IMPORT);
        config.setDataTransferFormat(format);
        config.setTransferData(transferData);
        config.setTransferDDL(transferDdl);
        if (external) {
            config.setTransferData(true);
            config.setTransferDDL(false);
        }
        config.setBatchCommitNum(100);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("TAB");
        object.setDbObjectType(ObjectType.TABLE);
        config.setExportDbObjects(Collections.singletonList(object));
        config.setReplaceSchemaWhenExists(true);
        config.setTruncateTableBeforeImport(true);
        config.setCsvConfig(new CsvConfig());
        if (format == DataTransferFormat.CSV) {
            CsvColumnMapping mapping = new CsvColumnMapping();
            mapping.setSrcColumnPosition(1);
            mapping.setSrcColumnName("COL1");
            mapping.setFirstLineValue("abc");
            mapping.setDestColumnPosition(0);
            mapping.setDestColumnName("COL2");
            config.setCsvColumnMappings(Collections.singletonList(mapping));
        }
        if (external) {
            config.setFileType(format.name());
        }
        return config;
    }

    private File getWorkingDir() {
        URL url = DumperOutput.class.getClassLoader().getResource("datatransfer");
        assert url != null;
        return new File(url.getPath());
    }

    private File getDumpZip() {
        URL url = DumperOutput.class.getClassLoader().getResource("datatransfer/export.zip");
        assert url != null;
        return new File(url.getPath());
    }

    private ConnectionConfig getConnectionConfig() {
        return getConnectionConfig(DialectType.OB_ORACLE);
    }

    private ConnectionConfig getConnectionConfig(DialectType dialectType) {
        return TestConnectionUtil.getTestConnectionConfig(ConnectType.from(dialectType));
    }

}
