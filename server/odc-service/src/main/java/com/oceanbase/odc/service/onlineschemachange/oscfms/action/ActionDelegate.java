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

import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;

/**
 * delegate impl
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 20:06
 * @since 4.3.1
 */
public class ActionDelegate implements Action<OscActionContext, OscActionResult> {
    // use delegate
    protected Action<OscActionContext, OscActionResult> omsAction;
    protected Action<OscActionContext, OscActionResult> odcAction;

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        return chooseAction(context).execute(context);
    }

    @Override
    public void rollback(OscActionContext context) {
        chooseAction(context).rollback(context);
    }

    protected Action<OscActionContext, OscActionResult> chooseAction(OscActionContext context) {
        if (context.getTaskParameter().isUseODCMigrateTool()) {
            return odcAction;
        } else {
            return omsAction;
        }
    }
}
