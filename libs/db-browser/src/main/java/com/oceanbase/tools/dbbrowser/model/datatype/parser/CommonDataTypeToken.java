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
package com.oceanbase.tools.dbbrowser.model.datatype.parser;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * {@link CommonDataTypeToken}
 *
 * @author yh263208
 * @date 2022-06-27 11:07
 * @since ODC_release_3.4.0
 * @see DataTypeToken
 */
public class CommonDataTypeToken implements Serializable, DataTypeToken {

    private final int start;
    private final int stop;
    private final int type;
    private final String text;

    public CommonDataTypeToken(@NonNull String text, int start, int stop, int type) {
        Validate.isTrue(start >= 0, "Start index can not be negative");
        Validate.isTrue(stop >= start, "Stop index can not be smaller than start index");
        this.start = start;
        this.stop = stop;
        this.text = text;
        this.type = type;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public int getStartIndex() {
        return this.start;
    }

    @Override
    public int getStopIndex() {
        return this.stop;
    }

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public String toString() {
        String txt = this.getText();
        if (txt != null) {
            txt = txt.replace("\n", "\\n");
            txt = txt.replace("\r", "\\r");
            txt = txt.replace("\t", "\\t");
        } else {
            txt = "<no text>";
        }
        String typeString = String.valueOf(this.type);
        return "[" + this.start + ":" + this.stop + "='" + txt + "',<" + typeString + ">" + "]";
    }
}
