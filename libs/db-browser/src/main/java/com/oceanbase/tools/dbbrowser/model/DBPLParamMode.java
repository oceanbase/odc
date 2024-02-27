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
package com.oceanbase.tools.dbbrowser.model;

import lombok.Getter;

@Getter
public enum DBPLParamMode {
    /**
     * 输入参数
     */
    IN,
    /**
     * 输出参数
     */
    OUT,
    /**
     * 输入输出参数
     */
    INOUT,
    /**
     * 未知类型
     */
    UNKNOWN;

    public static DBPLParamMode getEnum(String name) {
        if (PLConstants.PL_IN_PARAM.equalsIgnoreCase(name)) {
            return IN;
        } else if (PLConstants.PL_OUT_PARAM.equalsIgnoreCase(name)) {
            return OUT;
        } else if (PLConstants.OB_ORACLE_PL_INOUT_PARAM.equalsIgnoreCase(name)
                || PLConstants.MYSQL_PL_INOUT_PARAM.equalsIgnoreCase(name)
                || PLConstants.ORACLE_PL_INOUT_PARAM.equalsIgnoreCase(name)) {
            return INOUT;
        } else {
            return UNKNOWN;
        }
    }

    public boolean isOutParam() {
        return this == OUT || this == INOUT;
    }
}

