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

package com.oceanbase.odc.service.onlineschemachange.validator;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

/**
 * @author yaobin
 * @date 2023-08-11
 * @since 4.2.0
 */
@Component
public class WebOscConnectionConfigValidator implements OscConnectionConfigValidator {
    @Override
    public void valid(ConnectionConfig config) {
        validateConnectionConfig(config);
        validateSysTenant(config);
    }

    private void validateConnectionConfig(ConnectionConfig connectionConfig) {
        PreConditions.notNull(connectionConfig.getClusterName(), "clusterName");
        PreConditions.notNull(connectionConfig.getTenantName(), "tenantName");
        PreConditions.notNull(connectionConfig.getSysTenantUsername(), "sysTenantUsername");
    }

    private void validateSysTenant(ConnectionConfig connectionConfig) {
        PreConditions.notNull(connectionConfig.getSysTenantUsername(), "sysTenantUsername");
    }
}
