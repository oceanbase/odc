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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;

/**
 * {@link WebModeSingleOrganizationMetaDB}
 *
 * @author yh263208
 * @date 2022-05-19 14:07
 * @since ODC_release_3.3.1
 */
@Configuration
@Profile(value = {"alipay"})
@Component("metadbMigrate")
@DependsOn({"localObjectStorageFacade", "springContextUtil"})
@ConditionalOnProperty(value = "odc.iam.auth.type", havingValues = {"local", "alipay"})
public class WebModeSingleOrganizationMetaDB extends AbstractWebModeMetaDB {

    @Override
    protected List<ResourceConfig> resourceConfigs() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, 1L);
        parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, 1L);
        ResourceConfig resourceConfig = ResourceConfig.builder()
                .valueEncoderFactory(new DefaultValueEncoderFactory())
                .valueGeneratorFactory(new DefaultValueGeneratorFactory())
                .variables(parameters).build();
        return Collections.singletonList(resourceConfig);
    }

}
