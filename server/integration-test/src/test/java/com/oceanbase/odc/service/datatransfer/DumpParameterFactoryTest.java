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
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.model.CsvConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.service.datatransfer.model.DataTransferType;
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

    @Autowired
    private DataMaskingService maskingService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void dumpParameterFactory_negativeDumpSize_expThrown() throws FileNotFoundException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Max dump size can not be negative");
        new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(), -100L, 1000, maskingService);
    }

    @Test
    public void generate_positiveDumpSize_returnSettings() throws IOException {
        long maxDumpSize = 1000L;
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(), maxDumpSize, 1000,
                        maskingService);
        DumpParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, true, true));
        Assert.assertEquals(maxDumpSize, parameter.getMaxFileSize());
    }

    @Test
    public void generate_setTransferFormat_returnCsvFormat() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(), null, 1000,
                        maskingService);
        DumpParameter parameter = factory.generate(generateConfig(DataTransferFormat.CSV, true, true));
        Assert.assertEquals(DataFormat.CSV, parameter.getDataFormat());
    }

    @Test
    public void generate_setTransferFormat_returnSqlFormat() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(), null, 1000,
                        maskingService);
        DumpParameter parameter = factory.generate(generateConfig(DataTransferFormat.SQL, true, true));
        Assert.assertEquals(DataFormat.SQL, parameter.getDataFormat());
    }

    @Test
    public void generate_onlyIncludDdl_tabTableInWhiteMap() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_MYSQL),
                        null, 1000, maskingService);
        DumpParameter parameter = factory.generate(generateConfig(DataTransferFormat.SQL, false, true));
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("`TAB`"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyIncludDdl_ViewAndTableInWhiteMap() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_ORACLE),
                        null, 1000, maskingService);
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, false, true);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("V");
        object.setDbObjectType(ObjectType.VIEW);
        config.getExportDbObjects().add(object);
        DumpParameter parameter = factory.generate(config);
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("\"TAB\""));
        expect.putIfAbsent(ObjectType.VIEW, Collections.singleton("\"V\""));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyIncludData_whiteMapOnlyIncludeTable() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(DialectType.OB_MYSQL),
                        null, 1000, maskingService);
        DataTransferConfig config = generateConfig(DataTransferFormat.SQL, true, false);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName("V");
        object.setDbObjectType(ObjectType.VIEW);
        config.getExportDbObjects().add(object);
        DumpParameter parameter = factory.generate(config);
        Map<ObjectType, Set<String>> actual = parameter.getWhiteListMap();
        Map<ObjectType, Set<String>> expect = new HashMap<>();
        expect.putIfAbsent(ObjectType.TABLE, Collections.singleton("`TAB`"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_onlyIncludDdl_includeDdlIsTrue() throws IOException {
        DumpParameterFactory factory =
                new DumpParameterFactory(getWorkingDir(), getWorkingDir(), getConnectionConfig(), null, 1000,
                        maskingService);
        DumpParameter parameter = factory.generate(generateConfig(DataTransferFormat.SQL, false, true));
        Assert.assertTrue(parameter.isIncludeDdl());
    }

    private DataTransferConfig generateConfig(DataTransferFormat format,
            boolean transferData, boolean transferDdl) {
        DataTransferConfig config = new DataTransferConfig();
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
        URL url = DumperOutput.class.getClassLoader().getResource("datatransfer");
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
