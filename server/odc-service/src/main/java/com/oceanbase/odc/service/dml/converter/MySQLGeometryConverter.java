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

package com.oceanbase.odc.service.dml.converter;

import java.util.Arrays;
import java.util.Collection;

import com.oceanbase.odc.service.dml.DataValue;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/11/22
 * @since ODC_release_4.2.4
 */
public class MySQLGeometryConverter extends BaseDataConverter {
    @Override
    protected Collection<String> getSupportDataTypeNames() {
        return Arrays.asList("geometry", "point", "linestring", "polygon", "multipoint", "multilinestring",
                "multipolygon", "geometrycollection");
    }

    @Override
    protected String doConvert(@NonNull DataValue value) {
        String s = value.getValue();
        String[] parts = s.split("\\|");
        if (parts.length == 2) {
            String geometry = parts[0].trim();
            int srid = Integer.parseInt(parts[1].trim());
            return "ST_GeomFromText('" + geometry + "', " + srid + ")";
        } else if (parts.length == 1) {
            return "ST_GeomFromText('" + s + "')";
        } else {
            throw new IllegalArgumentException("Invalid geometry value: " + s);
        }
    }
}
