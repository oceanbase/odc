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

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsMonitorDataTaskAction;

/**
 * @author longpeng.zlp
 * @date 2024/7/8 19:59
 * @since 4.3.1
 */
@Component
public class MonitorDataTaskAction extends ActionDelegate {

    public static MonitorDataTaskAction ofOMSMonitorDataTaskAction(
            @NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        MonitorDataTaskAction ret = new MonitorDataTaskAction();
        ret.action = new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        return ret;
    }
}
