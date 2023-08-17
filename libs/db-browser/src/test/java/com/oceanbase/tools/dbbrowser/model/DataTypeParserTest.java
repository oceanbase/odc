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

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.parser.DataTypeParser;
import com.oceanbase.tools.dbbrowser.model.datatype.parser.DataTypeToken;
import com.oceanbase.tools.dbbrowser.model.datatype.parser.DataTypeTokenVisitor;

import lombok.NonNull;

/**
 * Test cases for {@link DataTypeParserTest}
 *
 * @author yh263208
 * @date 2022-06-27 14:45
 * @since ODC_release_3.4.0
 */
public class DataTypeParserTest {

    @Test
    public void getTokens_justName_returnOneToken() {
        String dataTypeName = "Binary_float";
        List<DataTypeToken> tokens = DataTypeParser.getTokens(dataTypeName);
        Assert.assertEquals(1, tokens.size());

        DataTypeToken token = tokens.get(0);
        Assert.assertEquals(dataTypeName, token.getText());
        Assert.assertEquals(DataTypeToken.NAME_TYPE, token.getType());
    }

    @Test
    public void getTokens_includeBrackets_returnTokens() {
        String typeName = "number (5,3)";
        List<DataTypeToken> tokens = DataTypeParser.getTokens(typeName);
        Assert.assertEquals(6, tokens.size());
    }

    @Test
    public void getTokens_withInvalidToken_retrunIncludeInvalidToken() {
        String typeName = "  number (5, -- 3 -) -";
        List<DataTypeToken> tokens = DataTypeParser.getTokens(typeName);
        Assert.assertEquals(10, tokens.size());
        Assert.assertEquals(5, tokens.stream().filter(token -> token.getType() == DataTypeToken.INVALID_TYPE).count());
    }

    @Test
    public void parse_validDataType_correctPared() {
        String dataType = "TIMESTAMP(6) WITH LOCAL TIME ZONE";
        DataTypeParser parser = new DataTypeParser(dataType);

        VisitorBasedDataType type = new VisitorBasedDataType();
        parser.parse(type);
        Assert.assertEquals("TIMESTAMP WITH LOCAL TIME ZONE", type.getDataTypeName());
        Assert.assertEquals(new Integer(6), type.getPrecision());
        Assert.assertEquals(new Integer(0), type.getScale());
    }
}


class VisitorBasedDataType implements DataType, DataTypeTokenVisitor {

    private final List<String> names = new LinkedList<>();
    private final List<Integer> numbers = new LinkedList<>();

    @Override
    public Integer getScale() {
        if (numbers.size() == 2) {
            return numbers.get(1);
        }
        return 0;
    }

    @Override
    public Integer getPrecision() {
        return numbers.get(0);
    }

    @Override
    public String getDataTypeName() {
        return String.join(" ", names);
    }

    @Override
    public void visitName(@NonNull DataTypeToken token) {
        names.add(token.getText());
    }

    @Override
    public void visitBrackets(@NonNull DataTypeToken token) {

    }

    @Override
    public void visitNumber(@NonNull DataTypeToken token) {
        numbers.add(Integer.parseInt(token.getText()));
    }

    @Override
    public void visitUnknown(@NonNull DataTypeToken token) {

    }
}

