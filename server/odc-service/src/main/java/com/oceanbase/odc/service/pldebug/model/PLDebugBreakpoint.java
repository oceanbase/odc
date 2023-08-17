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

import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PLDebugBreakpoint {
    private String packageName;
    private String objectName;
    /**
     * Currently we only support FUNCTION / PROCEDURE / ANONYMOUS_BLOCK
     */
    private DBObjectType objectType;
    private Integer lineNum;
    private Integer breakpointNum;

    public static PLDebugBreakpoint of(String objectName, DBObjectType objectType, Integer lineNum) {
        PLDebugBreakpoint plDebugBreakpoint = new PLDebugBreakpoint();
        plDebugBreakpoint.setObjectName(objectName);
        plDebugBreakpoint.setObjectType(objectType);
        plDebugBreakpoint.setLineNum(lineNum);
        return plDebugBreakpoint;
    }
}
