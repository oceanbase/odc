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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class MySQLReaderPluginParameter implements PluginParameter {

    private String username;
    private String password;
    /**
     * export all columns
     */
    private List<String> column = Collections.singletonList("*");
    private List<Connection> connection;
    // TODO private String splitPk;
    /**
     * must be singleton. If this field is specified, the DataX will ignore the other configurations
     * such as table and column, and directly use the content of this configuration to filter the data.
     */
    private List<String> querySql;

    @Data
    @AllArgsConstructor
    public static class Connection {
        private String[] jdbcUrl;
        private String[] table;
    }

}
