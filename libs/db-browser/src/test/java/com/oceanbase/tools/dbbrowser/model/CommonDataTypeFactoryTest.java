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
package com.oceanbase.tools.dbbrowser.model;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.GeneralDataType;

/**
 * Test cases for {@link CommonDataTypeFactory}
 *
 * @author yh263208
 * @date 2022-06-27 17:19
 * @since ODC_release_3.4.0
 */
public class CommonDataTypeFactoryTest {

    @Test
    public void generate_precisionScaleExists_returnNotNull() {
        String dataTypeStr = "number(5,3)";
        CommonDataTypeFactory factory = new CommonDataTypeFactory(dataTypeStr);
        DataType dataType = factory.generate();

        GeneralDataType expect = new GeneralDataType(5, 3, "number");
        Assert.assertEquals(expect, dataType);
    }

    @Test
    public void generate_precisionExists_returnScale() {
        String dataTypeStr = "TIMESTAMP(6) WITH LOCAL TIME ZONE";
        CommonDataTypeFactory factory = new CommonDataTypeFactory(dataTypeStr);
        DataType dataType = factory.generate();

        GeneralDataType expect = new GeneralDataType(6, 0, "TIMESTAMP WITH LOCAL TIME ZONE");
        Assert.assertEquals(expect, dataType);
    }

    @Test
    public void generate_precisionScaleNotExists_return0() {
        String dataTypeStr = "binary_float";
        CommonDataTypeFactory factory = new CommonDataTypeFactory(dataTypeStr);
        DataType dataType = factory.generate();

        GeneralDataType expect = new GeneralDataType(0, 0, "binary_float");
        Assert.assertEquals(expect, dataType);
    }

    @Test
    public void generate_includeNestedBrackets_returnEnum() {
        String dataTypeStr = "enum(('a'), 'b')";
        CommonDataTypeFactory factory = new CommonDataTypeFactory(dataTypeStr);
        DataType dataType = factory.generate();

        GeneralDataType expect = new GeneralDataType(0, 0, "enum");
        Assert.assertEquals(expect, dataType);
    }

}

