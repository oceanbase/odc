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
package com.oceanbase.odc.core.fsm;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sPodStatusFsm extends AbstractFsm<String, String> {

    public K8sPodStatusFsm(@NonNull List<FsmStateTransfer<String, String>> fsmStateTransfers) {
        super(fsmStateTransfers);
    }

    @Override
    protected void onStateTransfer(String currentState, String nextState, String s) throws Exception {
        log.info("Transfer state succeed, currentState={}, nextState={}, event={}", currentState, nextState, s);
    }

}
