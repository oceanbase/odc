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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.sql.execute.tool.StringCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * {@link OracleIntervalMapperTest}
 *
 * @author yh263208
 * @date 2022-07-14 13:20
 * @since ODC_rele
 */
public class OracleIntervalMapperTest {

    @Test
    public void mapCell_nonNullNegative_returnRightValue() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("INYRTVALDS");
        DataType dataType = factory.generate();
        OracleIntervalMapper mapper = new OracleIntervalMapper();
        String value = "-120000000-3";
        Assert.assertEquals(value, mapper.mapCell(new StringCellData(value, dataType)));
    }

    @Test
    public void mapCell_nonNullPositiveValue_returnRightValue() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("INYRTVALDS");
        DataType dataType = factory.generate();
        OracleIntervalMapper mapper = new OracleIntervalMapper();
        String value = "120000000-3";
        Assert.assertEquals("+" + value, mapper.mapCell(new StringCellData(value, dataType)));
    }

    @Test
    public void mapCell_nullValue_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("INTERVALDS");
        DataType dataType = factory.generate();
        OracleIntervalMapper mapper = new OracleIntervalMapper();
        Assert.assertNull(mapper.mapCell(new StringCellData(null, dataType)));
    }

    @Test
    public void supports_INTERVALDS_supports() throws IOException, SQLException {
        OracleIntervalMapper mapper = new OracleIntervalMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("INTERVALDS");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_INTERVALYM_supports() throws IOException, SQLException {
        OracleIntervalMapper mapper = new OracleIntervalMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("INTERVALYM");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

}
