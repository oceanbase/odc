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
package com.oceanbase.odc.service.tag;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class TagConfiguration {

    @Bean
    @ConditionalOnMissingBean(TagService.class)
    public TagService defaultTagService() {
        return new DefaultTagService();
    }

    @Bean
    @ConditionalOnMissingBean(TagServiceFacade.class)
    public TagServiceFacade defaultTagService(TagService tagService) {
        return new DefaultTagServiceFacadeImpl(tagService);
    }

}
