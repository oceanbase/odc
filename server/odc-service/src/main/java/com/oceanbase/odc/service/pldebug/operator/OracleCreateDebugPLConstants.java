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
package com.oceanbase.odc.service.pldebug.operator;

import com.oceanbase.odc.core.shared.constant.OdcConstants;

public class OracleCreateDebugPLConstants {
    final static String SET_BREAKPOINT =
            "PROCEDURE " + OdcConstants.PROCEDURE_SET_BREAKPOINT
                    + "(owner IN VARCHAR2, name IN VARCHAR2, line# IN BINARY_INTEGER, breakpoint# OUT BINARY_INTEGER, result OUT BINARY_INTEGER) IS"
                    + " pro_info dbms_debug.program_info;"
                    + " BEGIN"
                    + " pro_info.name := name;"
                    + " pro_info.owner := owner;"
                    + " result := dbms_debug.set_breakpoint(pro_info, line#, breakpoint#);"
                    + "END;";

    final static String SET_BREAKPOINT_ANONYMOUS =
            "PROCEDURE " + OdcConstants.PROCEDURE_SET_BREAKPOINT_ANONYMOUS
                    + "(line# IN BINARY_INTEGER, breakpoint# OUT BINARY_INTEGER, result OUT BINARY_INTEGER) IS"
                    + " run_info dbms_debug.runtime_info;"
                    + " BEGIN"
                    + " result := dbms_debug.get_runtime_info(dbms_debug.info_getLineinfo, run_info);"
                    + " result := dbms_debug.set_breakpoint(run_info.program, line#, breakpoint#);"
                    + "END;";

    final static String SHOW_BREAKPOINTS =
            "PROCEDURE " + OdcConstants.PROCEDURE_SHOW_BREAKPOINTS + "(listing in out varchar2) IS "
                    + "BEGIN"
                    + " dbms_debug.show_breakpoints(listing);"
                    + "END;";

    final static String PRINT_BACKTRACE =
            "PROCEDURE " + OdcConstants.PROCEDURE_PRINT_BACKTRACE
                    + "(listing IN OUT VARCHAR, status OUT BINARY_INTEGER) IS "
                    + " run_info dbms_debug.runtime_info;"
                    + " result BINARY_INTEGER;"
                    + "BEGIN "
                    + " result := dbms_debug.get_runtime_info(dbms_debug.info_getLineinfo, run_info);"
                    + " status := run_info.terminated;"
                    + " dbms_debug.print_backtrace(listing);"
                    + "END;";

    final static String CNT_NEXT_LINE =
            "PROCEDURE " + OdcConstants.PROCEDURE_CNT_NEXT_LINE + "(result OUT BINARY_INTEGER, message OUT VARCHAR2) IS"
                    + "  run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + "  result := dbms_debug.continue(run_info, dbms_debug.break_next_line);"
                    + "  message := ' run_info.breakpoint = ' || run_info.breakpoint || ', run_info.stackdepth = ' || run_info.stackdepth "
                    + "  || ', run_info.reason = ' || run_info.reason || ', run_info.programname = ' || run_info.program.name || ', run_info.programowner = ' || run_info.program.owner;"
                    + "END;";

    final static String CNT_NEXT_BREAKPOINT =
            "PROCEDURE " + OdcConstants.PROCEDURE_CNT_NEXT_BREAKPOINT
                    + "(result OUT BINARY_INTEGER, message OUT VARCHAR2) IS"
                    + "  run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + "  result := dbms_debug.continue(run_info, dbms_debug.break_any_return);"
                    + "  message := ' run_info.breakpoint = ' || run_info.breakpoint || ', run_info.stackdepth = ' || run_info.stackdepth "
                    + "  || ', run_info.reason = ' || run_info.reason || ', run_info.programname = ' || run_info.program.name || ', run_info.programowner = ' || run_info.program.owner;"
                    + "END;";

    final static String CNT_STEP_IN =
            "PROCEDURE " + OdcConstants.PROCEDURE_CNT_STEP_IN + "(result OUT BINARY_INTEGER, message OUT VARCHAR2) IS"
                    + "  run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + "  result := dbms_debug.continue(run_info, dbms_debug.break_any_call);"
                    + "  message := ' run_info.breakpoint = ' || run_info.breakpoint || ', run_info.stackdepth = ' || run_info.stackdepth "
                    + "  || ', run_info.reason = ' || run_info.reason || ', run_info.programname = ' || run_info.program.name || ', run_info.programowner = ' || run_info.program.owner;"
                    + "END;";

    final static String CNT_ABORT =
            "PROCEDURE " + OdcConstants.PROCEDURE_CNT_ABORT + "(result OUT BINARY_INTEGER, message OUT VARCHAR2) IS"
                    + "  run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + "  result := dbms_debug.continue(run_info, dbms_debug.abort_execution);"
                    + "  message := ' run_info.breakpoint = ' || run_info.breakpoint || ', run_info.stackdepth = ' || run_info.stackdepth "
                    + "  || ', run_info.reason = ' || run_info.reason || ', run_info.programname = ' || run_info.program.name || ', run_info.programowner = ' || run_info.program.owner;"
                    + "END;";

    private final static String CNT_STEP_OUT =
            "PROCEDURE " + OdcConstants.PROCEDURE_CNT_STEP_OUT + "(result OUT BINARY_INTEGER, message OUT VARCHAR2) IS"
                    + "  run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + "  result := dbms_debug.continue(run_info, dbms_debug.break_any_return);"
                    + "  message := ' run_info.breakpoint = ' || run_info.breakpoint || ', run_info.stackdepth = ' || run_info.stackdepth "
                    + "  || ', run_info.reason = ' || run_info.reason || ', run_info.programname = ' || run_info.program.name || ', run_info.programowner = ' || run_info.program.owner;"
                    + "END;";

    private final static String CNT_EXIT = "PROCEDURE " + OdcConstants.PROCEDURE_CNT_EXIT + "(message OUT VARCHAR2) IS"
            + "  run_info dbms_debug.runtime_info;"
            + "  result binary_integer;"
            + "BEGIN"
            + "  <<label>> loop"
            + "    result := dbms_debug.continue(run_info, dbms_debug.break_next_line);"
            + "    if run_info.reason = dbms_debug.reason_exit OR result != dbms_debug.success THEN"
            + "      exit label;"
            + "    end if;"
            + "  end loop label;"
            + "  message := ' reason = ' || run_info.reason;"
            + "END;";

    final static String GET_VALUES =
            "PROCEDURE " + OdcConstants.PROCEDURE_GET_VALUES
                    + "(scalar_values OUT VARCHAR2, result OUT BINARY_INTEGER) IS"
                    + " BEGIN"
                    + "  result := dbms_debug.get_values(scalar_values);"
                    + "END;";

    final static String GET_VALUE =
            "PROCEDURE " + OdcConstants.PROCEDURE_GET_VALUE
                    + "(variable_name VARCHAR2, frame# BINARY_INTEGER, value OUT VARCHAR2, result OUT BINARY_INTEGER) IS"
                    + " BEGIN"
                    + "  result := dbms_debug.get_value(variable_name, frame#, value);"
                    + "END;";

    final static String GET_RUNTIME_INFO =
            "PROCEDURE " + OdcConstants.PROCEDURE_GET_RUNTIME_INFO
                    + "(status OUT BINARY_INTEGER, result OUT BINARY_INTEGER) IS"
                    + " run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + " result := dbms_debug.get_runtime_info(dbms_debug.info_getLineinfo, run_info);"
                    + " status := run_info.terminated;"
                    + "END;";

    final static String SYNCHRONIZE =
            "PROCEDURE " + OdcConstants.PROCEDURE_SYNCHRONIZE
                    + "(result OUT BINARY_INTEGER, message OUT VARCHAR2) IS"
                    + " run_info dbms_debug.runtime_info;"
                    + "BEGIN"
                    + "  result := dbms_debug.synchronize(run_info, dbms_debug.info_getLineinfo);"
                    + "  message := ' run_info.breakpoint = ' || run_info.breakpoint || ', run_info.stackdepth = ' || run_info.stackdepth "
                    + "  || ', run_info.reason = ' || run_info.reason || ', run_info.programname = ' || run_info.program.name || ', run_info.programowner = ' || run_info.program.owner;"
                    + "END;";

    final static String GET_LINE =
            "PROCEDURE " + OdcConstants.PROCEDURE_GET_LINE + "(line OUT VARCHAR2, status OUT INTEGER) IS "
                    + "  log VARCHAR2(10000) := '';"
                    + "BEGIN"
                    + "  <<label>>"
                    + "  loop"
                    + "    dbms_output.get_line(line, status);"
                    + "    if status = 1 then"
                    + "      exit label;"
                    + "    else "
                    + "      log := log || '\n' || line;"
                    + "    end if;"
                    + "   end loop label;"
                    + " line := log;"
                    + "END;";

    public final static String WRAPPED_DEBUG_PL_PACKAGE_HEAD = "CREATE OR REPLACE PACKAGE "
            + OdcConstants.PL_DEBUG_PACKAGE + " AS"
            + " PROCEDURE " + OdcConstants.PROCEDURE_SET_BREAKPOINT
            +
            "(owner IN VARCHAR2, name IN VARCHAR2, line# IN BINARY_INTEGER, breakpoint# OUT BINARY_INTEGER, result OUT BINARY_INTEGER);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_SET_BREAKPOINT_ANONYMOUS
            + "(line# IN BINARY_INTEGER, breakpoint# OUT BINARY_INTEGER, result OUT BINARY_INTEGER);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_SHOW_BREAKPOINTS + "(listing in out varchar2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_PRINT_BACKTRACE
            + "(listing IN OUT VARCHAR, status OUT BINARY_INTEGER);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_CNT_NEXT_LINE
            + "(result OUT BINARY_INTEGER, message OUT VARCHAR2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_CNT_NEXT_BREAKPOINT
            + "(result OUT BINARY_INTEGER, message OUT VARCHAR2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_CNT_STEP_IN + "(result OUT BINARY_INTEGER, message OUT VARCHAR2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_CNT_ABORT + "(result OUT BINARY_INTEGER, message OUT VARCHAR2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_CNT_STEP_OUT + "(result OUT BINARY_INTEGER, message OUT VARCHAR2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_GET_VALUES
            + "(scalar_values OUT VARCHAR2, result OUT BINARY_INTEGER);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_GET_VALUE
            +
            "(variable_name VARCHAR2, frame# BINARY_INTEGER, value OUT VARCHAR2, result OUT BINARY_INTEGER);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_GET_RUNTIME_INFO
            + "(status OUT BINARY_INTEGER, result OUT BINARY_INTEGER);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_SYNCHRONIZE + "(result OUT BINARY_INTEGER, message OUT VARCHAR2);"
            + " PROCEDURE " + OdcConstants.PROCEDURE_GET_LINE +
            "(line OUT VARCHAR2, status OUT INTEGER);"
            + "END " + OdcConstants.PL_DEBUG_PACKAGE + ";";

    public final static String PL_DEBUG_PACKAGE_VERSION_NOTE = "-- ODC PL Debug Package Version: V3.3.2.1";

    final static String WRAPPED_DEBUG_PL_PACKAGE_BODY =
            "CREATE OR REPLACE PACKAGE BODY " + OdcConstants.PL_DEBUG_PACKAGE + " AS "
                    + PL_DEBUG_PACKAGE_VERSION_NOTE + "\n"
                    + SET_BREAKPOINT
                    + SET_BREAKPOINT_ANONYMOUS
                    + SHOW_BREAKPOINTS
                    + PRINT_BACKTRACE
                    + CNT_NEXT_LINE
                    + CNT_NEXT_BREAKPOINT
                    + CNT_STEP_IN
                    + CNT_ABORT
                    + CNT_STEP_OUT
                    + GET_VALUES
                    + GET_VALUE
                    + GET_RUNTIME_INFO
                    + SYNCHRONIZE
                    + GET_LINE
                    + "END " + OdcConstants.PL_DEBUG_PACKAGE + ";";

}
