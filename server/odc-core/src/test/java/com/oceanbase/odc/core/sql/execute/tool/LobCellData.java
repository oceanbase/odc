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
package com.oceanbase.odc.core.sql.execute.tool;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

public class LobCellData extends TestCellData {

    private final int streamSize;

    public LobCellData(int streamSize, @NonNull DataType dataType) {
        super(dataType);
        this.streamSize = streamSize;
    }

    public InputStream getBinaryStream() {
        if (streamSize <= 0) {
            return null;
        }
        return new ByteArrayInputStream(new byte[streamSize]);
    }

}


