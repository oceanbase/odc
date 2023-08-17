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

import java.util.List;

import com.oceanbase.odc.service.db.model.DBMSOutput;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/10/29
 */

@Data
public class PLDebugContextResp {
    private List<PLDebugVariable> variables;
    private List<DBPLError> errors;
    private DBMSOutput dbmsOutput;
    private boolean terminated;
    private PLDebugPrintBacktrace backtrace;

    // result
    private DBFunction functionResult;
    private List<DBPLParam> procedureResult;
}
