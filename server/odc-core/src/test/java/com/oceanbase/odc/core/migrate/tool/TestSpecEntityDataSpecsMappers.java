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
package com.oceanbase.odc.core.migrate.tool;

import javax.sql.DataSource;

import com.oceanbase.odc.core.migrate.resource.ResourceManager;
import com.oceanbase.odc.core.migrate.resource.mapper.TableSpecDataSpecsMapper;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;

import lombok.NonNull;

public class TestSpecEntityDataSpecsMappers {

    public static TableSpecDataSpecsMapper defaultMapper(@NonNull ResourceSpec defaultEntity,
            @NonNull ResourceManager manager, @NonNull DataSource dataSource) {
        return new TableSpecDataSpecsMapper(defaultEntity, new TestValueEncoderFactory(),
                new TestValueGeneratorFactory(), manager, dataSource);
    }

}
