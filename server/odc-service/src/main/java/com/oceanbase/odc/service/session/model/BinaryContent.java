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
package com.oceanbase.odc.service.session.model;

import com.oceanbase.odc.service.dml.ValueEncodeType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link BinaryContent}
 *
 * @author yh263208
 * @date 2022-04-27 14:32
 * @since ODC_release_3.3.1
 */
@Getter
@Setter
@EqualsAndHashCode
public class BinaryContent {

    private String content;
    private ValueEncodeType displayType;
    private long size;

    public BinaryContent() {}

    public BinaryContent(byte[] rawData, int size, @NonNull ValueEncodeType displayType) {
        this.size = size;
        this.content = null;
        if (rawData != null) {
            this.content = displayType.encodeToString(rawData);
        }
        this.displayType = displayType;
    }

    public static BinaryContent ofNull(@NonNull ValueEncodeType displayType) {
        return new BinaryContent(null, 0, displayType);
    }

}
