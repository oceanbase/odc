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
package com.oceanbase.odc.core.sql.execute.cache.model;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Binary data
 *
 * <pre>
 *     blob
 *     clob
 *     raw
 * </pre>
 *
 * needs to be stored on the local disk to save memory space.File storage needs some metadata
 * information, and this object is used to encapsulate this information
 *
 * @author yh263208
 * @date 2021-11-02 19:36
 * @since ODC_release_3.2.2
 */
@Getter
@ToString
@EqualsAndHashCode
public class BinaryContentMetaData implements Serializable {

    private final String filePath;
    private final int sizeInBytes;
    private final long offset;

    public BinaryContentMetaData(@NonNull String filePath, long offset, int sizeInBytes) {
        this.filePath = filePath;
        this.sizeInBytes = sizeInBytes;
        this.offset = offset;
    }

}

