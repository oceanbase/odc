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
import com.oceanbase.odc.service.onlineschemachange.oscfms.OSCActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OSCActionResult;

/**
 * delegate impl
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 20:06
 * @since 4.3.1
 */
public class ActionDelegate implements Action<OSCActionContext, OSCActionResult> {
    // use delegate
    protected Action<OSCActionContext, OSCActionResult> action;

    @Override
    public OSCActionResult execute(OSCActionContext context) throws Exception {
        return action.execute(context);
    }

    @Override
    public void rollback(OSCActionContext context) {
        action.rollback(context);
    }
}
