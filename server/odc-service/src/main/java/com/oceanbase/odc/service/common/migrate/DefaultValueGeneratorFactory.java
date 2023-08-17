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
package com.oceanbase.odc.service.common.migrate;

import com.oceanbase.odc.core.migrate.resource.factory.ValueGeneratorFactory;
import com.oceanbase.odc.core.migrate.resource.value.RandomStringGenerator;
import com.oceanbase.odc.core.migrate.resource.value.UuidGenerator;
import com.oceanbase.odc.core.migrate.resource.value.ValueGenerator;

import lombok.NonNull;

/**
 * {@link DefaultValueGeneratorFactory}
 *
 * @author yh263208
 * @date 2022-04-24 11:53
 * @since ODC_release_3.3.1
 * @see com.oceanbase.odc.core.migrate.resource.factory.ValueGeneratorFactory
 */
public class DefaultValueGeneratorFactory implements ValueGeneratorFactory {

    private static final String UUID_NAME = "UUID";
    private static final String ALPHA_NUMBERIC_NAME = "AlphaNumeric_32";

    @Override
    public ValueGenerator<?> generate(@NonNull GeneratorConfig config) {
        if (UUID_NAME.equalsIgnoreCase(config.getName())) {
            return new UuidGenerator();
        } else if (ALPHA_NUMBERIC_NAME.equalsIgnoreCase(config.getName())) {
            return new RandomStringGenerator();
        }
        throw new IllegalArgumentException("Illegal generator name " + config.getName());
    }

}
