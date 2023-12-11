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
package com.oceanbase.odc.service.common.util;

import java.util.Arrays;
import java.util.List;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.common.model.ResourceIdentifier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceIDParser {
    // order for this key list should not be modified
    // new key support: new key should insert into head of this list
    private static final List<String> keyList = Arrays.asList(ResourceIdentifier.TYPE_KEY,
            ResourceIdentifier.SYNONYM_KEY, ResourceIdentifier.TRIGGER_KEY,
            ResourceIdentifier.SEQUENCE_KEY, ResourceIdentifier.PACKAGE_KEY,
            ResourceIdentifier.PARTITION_KEY, ResourceIdentifier.PROCEDURE_KEY,
            ResourceIdentifier.FUNCTION_KEY, ResourceIdentifier.INDEX_KEY,
            ResourceIdentifier.COLUMN_KEY, ResourceIdentifier.VIEW_KEY,
            ResourceIdentifier.TABLE_KEY, ResourceIdentifier.VARIABLE_SCOPE_KEY,
            ResourceIdentifier.DATABASE_KEY, ResourceIdentifier.SID_KEY);

    public static ResourceIdentifier parse(String rId) {
        // sid represent static connection id like '1001', or dynamic session id like 'sid:1001-1'
        PreConditions.notEmpty(rId, "rId");

        ResourceIdentifier resourceIdentifier = new ResourceIdentifier();
        // set default sid
        resourceIdentifier.setSid(rId);
        Integer endIndex = rId.length();
        // sid:1:d:test
        for (String key : keyList) {
            try {
                endIndex = extract(rId, resourceIdentifier, endIndex, key);
            } catch (Exception e) {
                log.warn("Parse resource identifier failed, rId: {}, key: {}", rId, key, e);
                throw new BadRequestException("Parse resource identifier failed");
            }
        }
        return resourceIdentifier;
    }

    private static int extract(String rId, ResourceIdentifier resourceIdentifier, int endIndex, String key) {
        if (rId.contains(key)) {
            int startIndex = rId.indexOf(key);
            String value = rId.substring(startIndex + key.length(), endIndex);
            resourceIdentifier.setValue(key, value);
            endIndex = startIndex;
        }
        return endIndex;
    }
}
