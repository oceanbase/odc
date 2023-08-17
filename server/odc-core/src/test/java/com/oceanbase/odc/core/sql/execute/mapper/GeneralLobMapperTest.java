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

import com.oceanbase.odc.core.sql.execute.tool.LobCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * Test cases for {@link GeneralLobMapper}
 *
 * @author yh263208
 * @date 2022-07-14 12:11
 */
public class GeneralLobMapperTest {

    @Test
    public void mapCell_nonNullInputStream_returnRightValue() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("blob");
        DataType dataType = factory.generate();
        GeneralLobMapper mapper = new GeneralLobMapper();
        Assert.assertEquals("(blob) 12 B", mapper.mapCell(new LobCellData(12, dataType)));
    }

    @Test
    public void mapCell_nullInputStream_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("blob");
        DataType dataType = factory.generate();
        GeneralLobMapper mapper = new GeneralLobMapper();
        Assert.assertNull(mapper.mapCell(new LobCellData(-1, dataType)));
    }

    @Test
    public void supports_blob_supports() throws IOException, SQLException {
        GeneralLobMapper mapper = new GeneralLobMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("blob");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_clob_supports() throws IOException, SQLException {
        GeneralLobMapper mapper = new GeneralLobMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("clob");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_mediumblob_supports() throws IOException, SQLException {
        GeneralLobMapper mapper = new GeneralLobMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("mediumblob");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_tinyblob_supports() throws IOException, SQLException {
        GeneralLobMapper mapper = new GeneralLobMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("tinyblob");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_longblob_supports() throws IOException, SQLException {
        GeneralLobMapper mapper = new GeneralLobMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("longblob");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_timestamp_notSupports() throws IOException, SQLException {
        GeneralLobMapper mapper = new GeneralLobMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("timetamp(2) with local time zone");
        Assert.assertFalse(mapper.supports(factory.generate()));
    }

}

