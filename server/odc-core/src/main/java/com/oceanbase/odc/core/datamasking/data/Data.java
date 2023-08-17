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
package com.oceanbase.odc.core.datamasking.data;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.datamasking.data.metadata.FixedLengthStringMetadata;
import com.oceanbase.odc.core.datamasking.data.metadata.Metadata;

/**
 * @author wenniu.ly
 * @date 2022/8/23
 */

@lombok.Data
public class Data {
    private String value;
    private Metadata metadata;

    public static Data of(String value, Metadata metadata) {
        Data data = new Data();
        if (metadata instanceof FixedLengthStringMetadata) {
            value = StringUtils.stripEnd(value, null);
        }
        data.setValue(value);
        data.setMetadata(metadata);
        return data;
    }
}
