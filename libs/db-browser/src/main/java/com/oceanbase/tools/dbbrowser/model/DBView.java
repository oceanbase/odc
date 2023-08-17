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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DBView implements DBObject {

    private String viewName;
    // if null, use defaultSchemaName in current connection
    private String schemaName;
    private String ddl;
    private DBViewCheckOption checkOption;
    private boolean isUpdatable;
    private String definer;
    private String comment;
    private List<DBTableColumn> columns;

    // create parameters
    private List<DBViewUnit> viewUnits = new ArrayList<>();
    private List<DBViewColumn> createColumns;
    // TODO: need validate operations in mysql / oracle mode?
    private List<String> operations = new ArrayList<>();

    public void setCheckOption(String checkOption) {
        this.checkOption = DBViewCheckOption.valueOf(checkOption);
    }

    public static DBView of(String schemaName, String viewName) {
        // PreConditions.notBlank(viewName, "viewName");
        DBView dbView = new DBView();
        dbView.setSchemaName(schemaName);
        dbView.setViewName(viewName);
        return dbView;
    }

    @Override
    public String name() {
        return this.viewName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.VIEW;
    }

    /**
     * 视图创建单元参数
     */
    @Getter
    @Setter
    public static class DBViewUnit {
        private String dbName;
        private String tableName;
        private String tableAliasName;
    }

}

