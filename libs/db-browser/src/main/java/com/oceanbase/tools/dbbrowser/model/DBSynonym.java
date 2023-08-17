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

import lombok.Getter;
import lombok.Setter;

/**
 * ODC同义词对象信息封装体
 *
 * @author yh263208
 * @date 2020-12-19 16:45
 * @since ODC_release_2.4.0
 */
@Getter
@Setter
public class DBSynonym implements DBObject {

    private String owner;
    private String synonymName;
    private String tableOwner;
    private String tableName;
    private String dbLink;
    private Timestamp created;
    private Timestamp lastDdlTime;
    private String status;
    private DBSynonymType synonymType;
    private String ddl;

    @Override
    public String name() {
        return this.synonymName;
    }

    @Override
    public DBObjectType type() {
        return this.synonymType == DBSynonymType.COMMON ? DBObjectType.SYNONYM : DBObjectType.PUBLIC_SYNONYM;
    }

}

