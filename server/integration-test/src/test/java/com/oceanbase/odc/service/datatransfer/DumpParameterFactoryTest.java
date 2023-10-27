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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory.DumpParameterFactory;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;

/**
 * Test cases for {@link DumpParameterFactory}
 *
 * @author yh263208
 * @date 2022-07-27 16:06
 * @since ODC_release_3.4.0
 */
public class DumpParameterFactoryTest extends ServiceTestEnv {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void dumpParameterFactory_negativeDumpSize_expThrown() throws FileNotFoundException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Max dump size can not be negative");
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, true, true, DialectType.OB_ORACLE);
        config.setMaxDumpSizeBytes(-100L);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());    }

    @Test
    public void generate_positiveDumpSize_returnSettings() throws IOException {
        long maxDumpSize = 1000L;
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true, DialectType.OB_ORACLE);
        config.setMaxDumpSizeBytes(maxDumpSize);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());        DumpParameter parameter = factory.generate();
        Assert.assertEquals(maxDumpSize, parameter.getMaxFileSize());
    }

    @Test
    public void generate_setTransferFormat_returnCsvFormat() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.CSV, true, true,
                DialectType.OB_ORACLE);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());        DumpParameter parameter = factory.generate();
        Assert.assertEquals(DataFormat.CSV, parameter.getDataFormat());
    }

    @Test
    public void generate_setTransferFormat_returnSqlFormat() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, true, true,
                DialectType.OB_ORACLE);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());        DumpParameter parameter = factory.generate();
        Assert.assertEquals(DataFormat.SQL, parameter.getDataFormat());
    }

    @Test
    public void generate_onlyIncludDdl_tabTableInWhiteMap() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, true,
                DialectType.OB_MYSQL);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());        DumpParameter parameter = factory.generate();
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("`TAB`"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyIncludDdl_ViewAndTableInWhiteMap() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("V");
        object.setDbObjectType(ObjectType.VIEW);
        config.getExportDbObjects().add(object);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());        DumpParameter parameter = factory.generate();
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("\"TAB\""));
        expect.putIfAbsent(ObjectType.VIEW, Collections.singleton("\"V\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyIncludData_whiteMapOnlyIncludeTable() throws IOException {
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, true, false,
                DialectType.OB_MYSQL);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("V");
        object.setDbObjectType(ObjectType.VIEW);
        config.getExportDbObjects().add(object);

        DumpParameterFactory factory =
            new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                getWorkingDir(), getWorkingDir());        DumpParameter parameter = factory.generate();
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("`TAB`"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyIncludDdl_includeDdlIsTrue() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(generateConfig(DataTransferFormat.SQL, false, true, DialectType.OB_ORACLE),
                        getWorkingDir(), getWorkingDir());
        DumpParameter parameter = factory.generate();
        Assert.assertTrue(parameter.isIncludeDdl());
    }

    private DataTransferConfig generateConfig(DataTransferFormat format,
            boolean transferData, boolean transferDdl, DialectType dialectType) {
        DataTransferConfig config = new DataTransferConfig();
        config.setConnectionInfo(getConnectionInfo(dialectType));
        config.setSchemaName("test");
        config.setTransferType(DataTransferType.EXPORT);
        config.setDataTransferFormat(format);
        config.setTransferData(transferData);
        config.setTransferDDL(transferDdl);
        config.setBatchCommitNum(100);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("TAB");
        object.setDbObjectType(ObjectType.TABLE);
        config.setExportDbObjects(new LinkedList<>(Collections.singleton(object)));
        config.setReplaceSchemaWhenExists(true);
        config.setTruncateTableBeforeImport(true);
        config.setCsvConfig(new CsvConfig());
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
