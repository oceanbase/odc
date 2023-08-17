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
import lombok.Setter;

@Getter
@Setter
public class DBSequence implements DBObject {

    private String user;
    private String name;
    private String startValue;
    private String nextCacheValue;
    private Long increament;
    private String maxValue;
    private String minValue;
    private Boolean cached;
    private Long cacheSize;
    private Boolean orderd;
    private Boolean cycled;
    private String ddl;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.SEQUENCE;
    }

}
