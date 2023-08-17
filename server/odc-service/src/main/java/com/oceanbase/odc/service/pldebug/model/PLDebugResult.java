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

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;

import lombok.Data;
import lombok.ToString;

/**
 * @author wenniu.ly
 * @date 2021/11/10
 */

@Data
@ToString
public class PLDebugResult {
    private List<DBPLParam> procedureResult;
    private DBFunction functionResult;
    private String executionErrorMessage;

    public static PLDebugResult of(List<DBPLParam> procedureResult) {
        PLDebugResult plDebugResult = new PLDebugResult();
        plDebugResult.setProcedureResult(procedureResult);
        return plDebugResult;
    }

    public static PLDebugResult of(DBFunction functionResult) {
        PLDebugResult plDebugResult = new PLDebugResult();
        plDebugResult.setFunctionResult(functionResult);
        return plDebugResult;
    }

    public static PLDebugResult of(String executionErrorMessage) {
        PLDebugResult plDebugResult = new PLDebugResult();
        plDebugResult.setExecutionErrorMessage(executionErrorMessage);
        return plDebugResult;
    }
}
