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
package com.oceanbase.odc.service.lab.model;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@RefreshScope
public class LabProperties {
    @Value("${odc.lab.enabled}")
    private boolean labEnabled;

    @Value("${odc.lab.tutorial.enabled}")
    private boolean tutorialEnabled;

    @Value("${odc.lab.session-limit.enabled}")
    private boolean sessionLimitEnabled;

    @Value("${odc.lab.apply-permission.hidden}")
    private boolean applyPermissionHidden;

    @Value("${odc.lab.schedule.user-expired-time-millis:180000}")
    private Long userExpiredTime;

    @Value("${odc.lab.resource.mysql-init-script-template}")
    private String initMysqlResourceInitScriptTemplate;

    @Value("${odc.lab.resource.mysql-revoke-script-template}")
    private String initMysqlResourceRevokeScriptTemplate;

    @Value("${odc.lab.resource.oracle-init-script-template}")
    private String initOracleResourceInitScriptTemplate;

    @Value("${odc.lab.resource.oracle-revoke-script-template}")
    private String initOracleResourceRevokeScriptTemplate;

    @Value("${odc.lab.admin-id:}")
    private Set<String> adminIds;

    @Value("${odc.lab.ob.connection.key:}")
    private String obConnectionKey;

}
