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
package com.oceanbase.odc.core.datamasking.masker;

import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.data.Data;
import com.oceanbase.odc.core.datamasking.data.metadata.Metadata;
import com.oceanbase.odc.core.datamasking.data.metadata.MetadataFactory;

/**
 * @author wenniu.ly
 * @date 2022/8/25
 */
public class SingleValueDataMasker extends AbstractDataMasker {
    public SingleValueDataMasker(MaskConfig maskConfig) {
        super(maskConfig);
    }

    @Override
    public String mask(String origin, ValueMeta valueMeta) {
        Metadata metadata = MetadataFactory.createMetadata(valueMeta.getFieldName(), valueMeta.getDataType());
        Data data = Data.of(origin, metadata);
        return selector.mask(data).getValue();
    }
}
