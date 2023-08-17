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

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DBType implements DBObject {

    private String typeName;
    private String owner;
    private String type;
    private Date createTime;
    private Date lastDdlTime;
    private String ddl;

    private String typeId;
    private DBBasicPLObject typeDetail;
    private DBTypeCode typeCode;

    private String status;
    private String errorMessage;

    public static DBType of(String name, String ddl) {
        DBType type = new DBType();
        type.setTypeName(name);
        type.setDdl(ddl);
        return type;
    }

    @Override
    public String name() {
        return this.typeName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.TYPE;
    }

}
