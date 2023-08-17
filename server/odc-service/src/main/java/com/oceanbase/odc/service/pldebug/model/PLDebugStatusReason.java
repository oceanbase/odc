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
package com.oceanbase.odc.service.pldebug.model;

import lombok.Getter;

/**
 * @author wenniu.ly
 * @date 2022/7/29
 *
 *       Refer from: dbms_debug.sql
 */
public enum PLDebugStatusReason {
    reason_none(0, "reason_none", ""),
    reason_interpreter_starting(2, "reason_interpreter_starting", ""),
    reason_breakpoint(3, "reason_breakpoint", "at a breakpoint"),
    reason_enter(6, "reason_enter", "procedure entry"),
    reason_return(7, "reason_return", "procedure return"),
    reason_finish(8, "reason_finish", "procedure is finished"),
    reason_line(9, "reason_line", "reached a new line"),
    reason_interrupt(10, "reason_interrupt", "an interrupt occurred"),
    reason_exception(11, "reason_exception", "an exception was raised"),
    reason_exit(15, "reason_exit", "interpreter is exiting"),
    reason_handler(16, "reason_handler", "start exception-handler"),
    reason_timeout(17, "reason_timeout", "a timeout occurred"),
    reason_instantiate(20, "reason_instantiate", "instantiation block"),
    reason_abort(21, "reason_abort", "interpeter is aborting"),
    // 25 has been deprecated
    reason_knl_exit(25, "reason_knl_exit", "interpreter is exiting");

    PLDebugStatusReason(int id, String name, String desc) {
        this.name = name;
        this.id = id;
        this.desc = desc;
    }

    private String name = "";
    @Getter
    private int id;
    @Getter
    private String desc;

    public static PLDebugStatusReason getEnumById(int id) {
        PLDebugStatusReason[] reasons = PLDebugStatusReason.values();
        for (PLDebugStatusReason reason : reasons) {
            if (id == reason.getId()) {
                return reason;
            }
        }
        return PLDebugStatusReason.reason_none;
    }

    public boolean hasExited() {
        return this == reason_exit;
    }
}
