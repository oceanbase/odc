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

import com.oceanbase.jdbc.extend.datatype.TIMESTAMPTZ;
import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.core.sql.execute.tool.TimestampTZCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * Test cases for {@link OBOracleNlsFormatTimestampTZMapper}
 *
 * @author yh263208
 * @date 2023-07-04 22:07
 * @since ODC_release_4.2.0
 */
public class OBOracleNlsFormatTimestampTZMapperTest {

    @Test
    public void mapCell_nonNullTimestampTZ_returnRightValue() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp(3) with time zone");
        DataType dataType = factory.generate();
        OBOracleNlsFormatTimestampTZMapper mapper = new OBOracleNlsFormatTimestampTZMapper("DD-MM-RR TZH TZM");
        Object actual = mapper.mapCell(new TimestampTZCellData(new TIMESTAMPTZ(), dataType));
        TimeFormatResult expect = new TimeFormatResult("01-01-70 00 00", 3661000L, 0, "+00:00");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void mapCell_nullTimestampTZ_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp(3) with time zone");
        DataType dataType = factory.generate();
        OBOracleNlsFormatTimestampTZMapper mapper = new OBOracleNlsFormatTimestampTZMapper("DD-MM-RR TZH TZM");
        Assert.assertNull(mapper.mapCell(new TimestampTZCellData(null, dataType)));
    }

    @Test
    public void supports_timestampTZ_supports() throws IOException, SQLException {
        OBOracleNlsFormatTimestampTZMapper mapper = new OBOracleNlsFormatTimestampTZMapper("DD-MM-RR TZH TZM");
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp(3) with time zone");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_timestampLTZ_notSupports() throws IOException, SQLException {
        OBOracleNlsFormatTimestampTZMapper mapper = new OBOracleNlsFormatTimestampTZMapper("DD-MM-RR TZH TZM");
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp(2) with local time zone");
        Assert.assertFalse(mapper.supports(factory.generate()));
    }
}
