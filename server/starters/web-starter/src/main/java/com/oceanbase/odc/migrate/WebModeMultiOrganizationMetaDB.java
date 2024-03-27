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
package com.oceanbase.odc.migrate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.IgnoreResourceIdHandle;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;

/**
 * {@link WebModeMultiOrganizationMetaDB}
 *
 * @author yh263208
 * @date 2022-05-19 14:17
 * @since ODC_release_3.3.1
 * @see AbstractWebModeMetaDB
 */
@Configuration
@Profile(value = {"alipay"})
@Component("metadbMigrate")
@DependsOn({"localObjectStorageFacade", "springContextUtil"})
// can't remove ，cause migrate is after environment properties set.
@ConditionalOnProperty(value = "odc.iam.auth.type",
        havingValues = {"buc", "oauth2"}, collectionProperty = true)
public class WebModeMultiOrganizationMetaDB extends AbstractWebModeMetaDB {

    @Override
    protected List<ResourceConfig> resourceConfigs() {
        return getOrganizationId2CreatorId().entrySet().stream().map(entry -> {
            Map<String, Object> parameters = new HashMap<>();
            parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, entry.getValue());
            parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, entry.getKey());
            return ResourceConfig.builder()
                    .valueEncoderFactory(new DefaultValueEncoderFactory())
                    .valueGeneratorFactory(new DefaultValueGeneratorFactory())
                    .handle(new IgnoreResourceIdHandle())
                    .variables(parameters).build();
        }).collect(Collectors.toList());
    }

}
