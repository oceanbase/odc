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

import lombok.extern.slf4j.Slf4j;

/**
 * Created by mogao.zj
 *
 * Refer from: dbms_debug.sql
 */
@Slf4j
public enum PLDebugErrorCode {
    unknown(-1, "unknown", "unknown exception"),
    success(0, "success", ""),
    // returned by GET_VALUE and SET_VALUE
    error_bogus_frame(1, "error_bogus_frame", "Frame does not exist"),
    error_no_debug_info(2, "error_no_debug_info", "Entrypoint has no debug information"),
    error_no_such_object(3, "error_no_such_object", "variable_name does not exist in frame#"),
    error_unknown_type(4, "error_unknown_type", "The type information in the debug information is illegible"),
    error_indexed_table(18, "error_indexed_table", "The object is a table, but no index was provided"),
    error_illegal_index(19, "error_illegal_index", "Illegal collection index"),
    error_nullvalue(32, "error_nullvalue", "Value is NULL"),
    error_nullcollection(40, "error_nullcollection", "Collection is atomically null"),
    // returned by SET_VALUE
    error_illegal_value(5, "error_illegal_value", "Constraint violation"),
    error_illegal_null(6, "error_illegal_null", "Constraint violation"),
    error_value_malformed(7, "error_value_malformed", "Bad value"),
    error_other(8, "error_other", "Unknown error"),
    error_name_incomplete(11, "error_name_incomplete", "Not a scalar lvalue"),
    // returned by the breakpoint functions
    error_illegal_line(12, "error_illegal_line", "No such line"),
    error_no_such_breakpt(13, "error_no_such_breakpt", "No such breakpoint exists"),
    error_idle_breakpt(14, "error_idle_breakpt", "Cannot delete an unused breakpoint"),
    error_stale_breakpt(15, "error_stale_breakpt", "The program unit was redefined since the breakpoint was set"),
    error_bad_handle(16, "error_bad_handle", "Can not set breakpoint here"),
    // General error codes
    error_unimplemented(17, "error_bad_handle", "NYI functionality"),
    error_deferred(27, "error_bad_handle", "Request was deferred"),
    error_exception(28, "error_bad_handle", "Exception inside Probe"),
    error_communication(29, "error_bad_handle", "Generic pipe error"),
    error_timeout(31, "error_bad_handle", "Timeout"),
    ;

    private String name = "";
    private int id;
    private String desc;

    PLDebugErrorCode(int id, String name, String desc) {
        this.name = name;
        this.id = id;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public static PLDebugErrorCode getEnumById(int id) {
        PLDebugErrorCode[] types = PLDebugErrorCode.values();
        for (PLDebugErrorCode type : types) {
            if (id == type.getId()) {
                return type;
            }
        }
        log.warn("Unknown DbmsDebugException, value={}", id);
        return PLDebugErrorCode.unknown;
    }

    public String getDebugErrorCode(int ret) {
        if (this != PLDebugErrorCode.unknown) {
            return getName();
        }
        return getName() + " [ret=" + ret + "]";
    }

    public String getDebugErrorMessage(int ret) {
        if (this != PLDebugErrorCode.unknown) {
            return getDesc();
        }
        return getDesc() + " [ret=" + ret + "], please check observer version";
    }
}
