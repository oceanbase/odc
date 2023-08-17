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

import lombok.Data;

/**
 * {@link DBRoutineCharacteristic}
 *
 * @author jingtian
 * @date 2023/4/11
 * @since db-browser_1.0.0-SNAPSHOT
 */
@Data
public class DBRoutineCharacteristic {
    /**
     * 决定性，例程是否总是对相同的输入参数产生相同的结果
     */
    private Boolean deterministic;
    /**
     * 数据性质
     */
    private DBRoutineDataNature dataNature;
    /**
     * SQL 安全性
     */
    private DBPLSqlSecurity sqlSecurity;
    /**
     * 注释
     */
    private String comment;
}
