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

import java.io.InputStream;

import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;

import lombok.NonNull;

public class TestBinaryDataManager implements BinaryDataManager {

    @Override
    public BinaryContentMetaData write(@NonNull InputStream inputStream) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public InputStream read(@NonNull BinaryContentMetaData metaData) {
        throw new UnsupportedOperationException("read");
    }

    @Override
    public void close() {}

}
