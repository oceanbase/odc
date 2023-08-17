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

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class DBFunction extends DBBasicPLObject implements DBObject {

    private String packageName;
    private String funName;
    private String ddl;
    private String definer;
    private String status;
    private Timestamp createTime;
    private Timestamp modifyTime;
    private String returnType;
    private String returnValue;
    private String errorMessage;
    private boolean returnExtendedType;
    private DBRoutineCharacteristic characteristic;

    public static DBFunction of(@NonNull String funName,
            @NonNull String returnType) {
        DBFunction function = new DBFunction();
        function.funName = funName;
        function.returnType = returnType;
        return function;
    }

    @Override
    public String name() {
        return this.funName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.FUNCTION;
    }

}
