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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc.OdcCreateDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsCreateDataTaskAction;
import com.oceanbase.odc.service.resource.ResourceManager;

import lombok.NonNull;

/**
 * @author longpeng.zlp
 * @date 2024/7/8 17:57
 * @since 4.3.1
 */
public class CreateDataTaskAction extends ActionDelegate {

    public static CreateDataTaskAction createDataTaskAction(
            @NotNull DataSourceOpenApiService dataSourceOpenApiService,
            @NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties oscProperties,
            @NonNull SystemConfigService systemConfigService,
            @NonNull ResourceManager resourceManager) {
        CreateDataTaskAction ret = new CreateDataTaskAction();
        ret.omsAction = new OmsCreateDataTaskAction(dataSourceOpenApiService, projectOpenApiService, oscProperties);
        ret.odcAction = new OdcCreateDataTaskAction(systemConfigService, resourceManager, oscProperties);
        return ret;
    }
}
