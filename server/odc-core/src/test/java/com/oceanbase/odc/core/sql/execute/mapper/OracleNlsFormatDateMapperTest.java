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
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.core.sql.execute.tool.DateCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * Test cases for {@link OracleNlsFormatDateMapper}
 *
 * @author yh263208
 * @date 2023-07-04 22:19
 * @since ODC_release_4.2.0
 */
public class OracleNlsFormatDateMapperTest {

    @Test
    public void mapCell_nonNullMore1000YearDate_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("date");
        DataType dataType = factory.generate();
        OracleNlsFormatDateMapper mapper = new OracleNlsFormatDateMapper("DD-MM-RR");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "2022-07-14 12:27:11";
        Date date = new Date(format.parse(value).getTime());
        Object actual = mapper.mapCell(new DateCellData(date, dataType));
        TimeFormatResult expect = new TimeFormatResult("14-07-22", date.getTime());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void mapCell_nonNullLess1000YearDate_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("date");
        DataType dataType = factory.generate();
        OracleNlsFormatDateMapper mapper = new OracleNlsFormatDateMapper("DD-MM-RR");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "222-07-14 12:27:11";
        Date date = new Date(format.parse(value).getTime());
        Object actual = mapper.mapCell(new DateCellData(date, dataType));
        TimeFormatResult expect = new TimeFormatResult("14-07-22", date.getTime());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void mapCell_nullDate_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("date");
        DataType dataType = factory.generate();
        OracleNlsFormatDateMapper mapper = new OracleNlsFormatDateMapper("DD-MM-RR");
        Assert.assertNull(mapper.mapCell(new DateCellData(null, dataType)));
    }

    @Test
    public void supports_date_supports() throws IOException, SQLException {
        OracleNlsFormatDateMapper mapper = new OracleNlsFormatDateMapper("DD-MM-RR");
        DataTypeFactory factory = new CommonDataTypeFactory("date");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }
}
