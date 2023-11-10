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
package com.oceanbase.odc.plugin.task.obmysql.datatransfer;

import java.io.File;
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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.LoadParameterFactory;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
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
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, false, true, true, DialectType.OB_ORACLE);
        config.setStopWhenError(true);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(0, parameter.getMaxErrors());
    }

    @Test
    public void generate_doNotStopWhenErrorSetting_returnMaxInteger() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, false, true, true, DialectType.OB_ORACLE);
        config.setStopWhenError(false);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(-1, parameter.getMaxErrors());
    }

    @Test
    public void generate_externalSqlFormat_returnMix() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(DataFormat.MIX, parameter.getDataFormat());
    }

    @Test
    public void generate_sqlFormat_returnSql() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, true, false,
                DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(DataFormat.SQL, parameter.getDataFormat());
    }

    @Test
    public void generate_csvFormat_returnCsv() throws IOException {
        DataTransferConfig config =
                generateConfig(DataTransferFormat.CSV, false, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(DataFormat.CSV, parameter.getDataFormat());
    }

    @Test
    public void generate_nonExternalMysqlWhiteListMap_returnTESTTable() throws IOException {
        DataTransferConfig config =
                generateConfig(DataTransferFormat.CSV, false, false, true, DialectType.OB_MYSQL);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        for (DataTransferObject object : config.getExportDbObjects()) {
            Set<String> names =
                    expect.computeIfAbsent(object.getDbObjectType(), k -> new HashSet<>());
            names.add(StringUtils.quoteMysqlIdentifier(object.getObjectName()));
        }
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nonExternalOracleWhiteListMap_returnTESTTable() throws IOException {
        DataTransferConfig config =
                generateConfig(DataTransferFormat.CSV, false, false, true, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        for (DataTransferObject object : config.getExportDbObjects()) {
            Set<String> names =
                    expect.computeIfAbsent(object.getDbObjectType(), k -> new HashSet<>());
            names.add(StringUtils.quoteOracleIdentifier(object.getObjectName()));
        }
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_nonExternalIncludeDdl_returnTrue() throws IOException {
        DataTransferConfig config =
                generateConfig(DataTransferFormat.SQL, false, false, true, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertTrue(parameter.isIncludeDdl());
    }

    @Test
    public void generate_externalCsvCsvMappingConfig_returnTrue() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
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
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("\"TAB\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_externalCsvInvalidObjectList_expThrown() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("TAB");
        object.setDbObjectType(ObjectType.VIEW);
        config.setExportDbObjects(Collections.singletonList(object));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No effective white list map settings");
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        factory.generate();
    }

    @Test
    public void generate_isNoSys_returnTrue() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        config.setSysUser(null);
        config.setSysPassword(null);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertTrue(parameter.isNoSys());
    }

    @Test
    public void generate_setCsvInfoDoubleExcapeN_returnExpectedCsvInfo() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setLineSeparator("\\n");
        config.setCsvConfig(csvConfig);
        LoadParameter parameter = factory.generate();
        Assert.assertEquals("\n", parameter.getLineSeparator());
    }

    @Test
    public void generate_setCsvInfoExcapeN_returnExpectedCsvInfo() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals("\n", parameter.getLineSeparator());
    }

    @Test
    public void generate_setCsvInfoDoubleExcapeR_returnExpectedCsvInfo() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setLineSeparator("\\r");
        config.setCsvConfig(csvConfig);
        LoadParameter parameter = factory.generate();
        Assert.assertEquals("\r", parameter.getLineSeparator());
    }

    @Test
    public void generate_blankToNull_returnNullEmptyReplacer() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setBlankToNull(true);
        config.setCsvConfig(csvConfig);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertNull(parameter.getEmptyReplacer());
        Assert.assertEquals("null", parameter.getNullString());
    }

    @Test
    public void generate_workingDirAssignedToFilePath_pathEquals() throws IOException {
        File workingDir = getWorkingDir();
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(workingDir.getAbsolutePath(), parameter.getFilePath());
    }

    @Test
    public void generate_fileEncoding_encodingEquals() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, false, DialectType.OB_ORACLE);
        LoadParameterFactory factory = new LoadParameterFactory(config, getWorkingDir(), getWorkingDir());
        LoadParameter parameter = factory.generate();
        Assert.assertEquals(config.getEncoding().getAlias(), parameter.getFileEncoding());
    }

    private DataTransferConfig generateConfig(DataTransferFormat format, boolean external,
            boolean transferData, boolean transferDdl, DialectType dialectType) {
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
        config.setConnectionInfo(getConnectionInfo(dialectType));
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
        } else {
            config.setFileType("ZIP");
        }
        return config;
    }

    private File getWorkingDir() {
        URL url = this.getClass().getClassLoader().getResource("datatransfer");
        assert url != null;
        return new File(url.getPath());
    }

    private ConnectionInfo getConnectionInfo(DialectType dialectType) {
        TestDBConfiguration configuration;
        if (dialectType.isMysql()) {
            configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        } else {
            configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        }
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setHost(configuration.getHost());
        connectionInfo.setPort(configuration.getPort());
        connectionInfo.setConnectType(ConnectType.from(dialectType));
        connectionInfo.setClusterName(configuration.getCluster());
        connectionInfo.setTenantName(configuration.getTenant());
        connectionInfo.setUsername(configuration.getUsername());
        connectionInfo.setPassword(configuration.getPassword());
        return connectionInfo;
    }

}
