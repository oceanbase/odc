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
package com.oceanbase.odc.service.onlineschemachange.oscfms;

import com.oceanbase.odc.service.onlineschemachange.fsm.StateTransfer;

/**
 * @author longpeng.zlp
 * @date 2024/7/9 17:54
 * @since 4.3.1
 */
public class OscStatesTransfer implements StateTransfer<OscActionContext, OscActionResult> {
    @Override
    public String translateToNewState(String currentState, OscActionResult oscActionResult, OscActionContext context) {
        return oscActionResult.getNextState();
    }
}
