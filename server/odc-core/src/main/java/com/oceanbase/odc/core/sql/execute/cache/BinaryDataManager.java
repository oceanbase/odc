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
package com.oceanbase.odc.core.sql.execute.cache;

import java.io.IOException;
import java.io.InputStream;

import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;

import lombok.NonNull;

/**
 * Binary data manager, used to persist binary data to disk, and provide upper-layer unaware binary
 * data writing and reading
 *
 * @author yh263208
 * @date 2021-11-03 17:21
 * @since ODC_release_3.2.2
 */
public interface BinaryDataManager extends AutoCloseable {
    /**
     * Data writing method, need to provide an input stream for receiving data
     *
     * @param inputStream {@code InputStream}
     * @return meta data of the binary data
     * @throws IOException some errors may happend
     */
    BinaryContentMetaData write(@NonNull InputStream inputStream) throws IOException;

    /**
     * Data reading method, returns an output stream
     *
     * @param metaData {@code BinaryTypeMetaData}
     * @return {@code InputStream}
     * @throws IOException some errors may happend
     */
    InputStream read(@NonNull BinaryContentMetaData metaData) throws IOException;

}

