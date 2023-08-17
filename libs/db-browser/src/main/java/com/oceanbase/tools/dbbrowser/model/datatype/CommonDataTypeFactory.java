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
package com.oceanbase.tools.dbbrowser.model.datatype;

import java.util.LinkedList;
import java.util.List;

import com.oceanbase.tools.dbbrowser.model.datatype.parser.DataTypeParser;
import com.oceanbase.tools.dbbrowser.model.datatype.parser.DataTypeToken;
import com.oceanbase.tools.dbbrowser.model.datatype.parser.DataTypeTokenVisitor;
import com.oceanbase.tools.dbbrowser.model.datatype.parser.DefaultDataTypeTokenVisitor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link CommonDataTypeFactory}
 *
 * @author yh263208
 * @date 2022-06-27 17:04
 * @since ODC_release_3.4.0
 * @see DataTypeFactory
 * @see DataTypeTokenVisitor
 */
@Slf4j
public class CommonDataTypeFactory extends DefaultDataTypeTokenVisitor implements DataTypeFactory {

    private final List<String> dataTypeNames = new LinkedList<>();
    private final List<Integer> scales = new LinkedList<>();
    private Integer bracketCounter = 0;

    /**
     * Default constructor, accept data type value, eg.
     * 
     * <pre>
     *     timestamp(6) with local time zone
     * </pre>
     *
     * @param dataType eg. {@code timestamp(6) with local time zone}
     */
    public CommonDataTypeFactory(@NonNull String dataType) {
        DataTypeParser parser = new DataTypeParser(DataTypeParser.getTokens(dataType));
        parser.parse(this);
    }

    @Override
    public DataType generate() {
        String dataTypeName = String.join(" ", dataTypeNames);
        if (scales.size() == 0) {
            return new GeneralDataType(dataTypeName);
        } else if (scales.size() == 1) {
            return new GeneralDataType(scales.get(0), dataTypeName);
        }
        return new GeneralDataType(scales.get(0), scales.get(1), dataTypeName);
    }

    @Override
    public void visitName(@NonNull DataTypeToken token) {
        if (bracketCounter <= 0) {
            dataTypeNames.add(token.getText());
        }
    }

    @Override
    public void visitNumber(@NonNull DataTypeToken token) {
        try {
            scales.add(Integer.parseInt(token.getText()));
        } catch (Exception e) {
            log.warn("Failed to parse integer, token={}", token, e);
        }
    }

    @Override
    public void visitBrackets(@NonNull DataTypeToken token) {
        if ("(".equals(token.getText())) {
            bracketCounter++;
        } else if (")".equals(token.getText())) {
            bracketCounter--;
        }
    }

}
