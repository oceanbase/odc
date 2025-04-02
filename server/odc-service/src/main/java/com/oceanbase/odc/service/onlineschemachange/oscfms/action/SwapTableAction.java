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

import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc.OdcSwapTableAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsSwapTableAction;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

/**
 * @author longpeng.zlp
 * @date 2024/7/9 11:57
 * @since 4.3.1
 */
public class SwapTableAction extends ActionDelegate {
    public static SwapTableAction ofOMSSwapTableAction(@NotNull DBSessionManageFacade dbSessionManageFacade,
            @NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        SwapTableAction ret = new SwapTableAction();
        ret.omsAction =
                new OmsSwapTableAction(dbSessionManageFacade, projectOpenApiService, onlineSchemaChangeProperties);
        ret.odcAction = new OdcSwapTableAction(dbSessionManageFacade, onlineSchemaChangeProperties);
        return ret;
    }
}
