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

@Data
public class DBDatabase {
    private String id;
    private String name;
    private String charset;
    private String size;
    private String collation;
    private Timestamp gmtCreated;
    private Timestamp gmtModified;

    public static DBDatabase of(String name) {
        DBDatabase database = new DBDatabase();
        database.setName(name);
        return database;
    }
}
