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
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DBProcedure extends DBBasicPLObject implements DBObject {

    private String packageName;
    private String proName;
    private String ddl;
    private String definer;
    private String status;
    private Timestamp createTime;
    private Timestamp modifyTime;
    private String errorMessage;
    private DBRoutineCharacteristic characteristic;

    public static DBProcedure of(String proName, String ddl) {
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(proName);
        procedure.setDdl(ddl);
        return procedure;
    }

    public static DBProcedure of(String packageName, String proName, List<DBPLParam> params) {
        DBProcedure procedure = new DBProcedure();
        procedure.setPackageName(packageName);
        procedure.setProName(proName);
        procedure.setParams(params);
        return procedure;
    }

    @Override
    public String name() {
        return this.proName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.PROCEDURE;
    }

}
