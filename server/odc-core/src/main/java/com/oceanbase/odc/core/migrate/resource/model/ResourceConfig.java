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
package com.oceanbase.odc.core.migrate.resource.model;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import com.oceanbase.odc.core.migrate.resource.factory.ValueEncoderFactory;
import com.oceanbase.odc.core.migrate.resource.factory.ValueGeneratorFactory;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link ResourceConfig}
 *
 * @author yh263208
 * @date 2022-04-24 22:53
 * @since ODC_release_3.3.1
 */
@Getter
@Setter
@Builder
public class ResourceConfig {

    private DataSource dataSource;

    private ValueEncoderFactory valueEncoderFactory;

    private ValueGeneratorFactory valueGeneratorFactory;

    private List<String> resourceLocations;

    private Map<String, ?> variables;

    @Builder.Default
    private Function<ResourceSpec, ResourceSpec> handle = entity -> entity;

}
