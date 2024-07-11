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
package com.oceanbase.odc.service.onlineschemachange.oscfms.state;

/**
 * State definition
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 10:40
 * @since 4.3.1
 */
public enum OSCStates {
    // init or yield fms and reset states. it's first state of fms
    YIELD_CONTEXT("YIELD_CONTEXT"),
    CREATE_GHOST_TABLES("CREATE_GHOST_TABLES"),
    CREATE_DATA_TASK("CREATE_DATA_TASK"),
    MONITOR_DATA_TASK("MONITOR_DATA_TASK"),
    MODIFY_DATA_TASK("MODIFY_DATA_TASK"),
    SWAP_TABLE("SWAP_TABLE"),
    CLEAN_RESOURCE("CLEAR_RESOURCE"),
    // all task finished or canceled
    COMPLETE("COMPLETE"),
    ;

    private final String state;

    OSCStates(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
