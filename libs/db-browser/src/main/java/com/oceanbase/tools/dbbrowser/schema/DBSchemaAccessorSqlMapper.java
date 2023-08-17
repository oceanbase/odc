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
package com.oceanbase.tools.dbbrowser.schema;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2023/1/29 15:06
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DBSchemaAccessorSqlMapper {
    private Map<String, String> sqls;

    public String getSql(String id) {
        String sql = sqls.get(id);
        Validate.notEmpty(sql, "sql(id=" + id + ")");
        return sql;
    }
}
