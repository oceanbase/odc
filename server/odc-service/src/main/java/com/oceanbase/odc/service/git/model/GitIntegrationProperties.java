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
package com.oceanbase.odc.service.git.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/9
 */
@Configuration
@RefreshScope
@Data
public class GitIntegrationProperties {

    @Value("${odc.integration.git.repository-retention-minutes:1440}")
    private Long gitRepositoryPreserveMinutes = 1440L;

    @Value("${odc.integration.git.repository-max-cached-size:1000}")
    private Long gitRepositoryMaxCachedSize = 1000L;

}
