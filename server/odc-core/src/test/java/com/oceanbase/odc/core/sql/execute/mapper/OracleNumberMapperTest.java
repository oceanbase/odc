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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.oceanbase.tools.dbbrowser.model.datatype.GeneralDataType;

@RunWith(Parameterized.class)
public class OracleNumberMapperTest {

    @Parameter(0)
    public String input;
    @Parameter(1)
    public String expect;

    @Parameters(name = "{index}: var[{0}]={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"1", "1"},
                {"0", "0"},
                {"0.123", "0.123"},
                {"1.23", "1.23"},
        });
    }

    @Test
    public void test_mapCell() throws Exception {
        GeneralDataType dataType = new GeneralDataType(10, 1, "NUMBER");
        CellData cellData = Mockito.mock(CellData.class);
        Mockito.when(cellData.getBigDecimal()).thenReturn(new BigDecimal(input));
        Mockito.when(cellData.getDataType()).thenReturn(dataType);
        Assert.assertEquals(expect, new OracleNumberMapper().mapCell(cellData).toString());
    }

}
