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
package com.oceanbase.tools.dbbrowser.schema.oracle;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;

/**
 * 适用于的 DB 版本：[4.0.0, 4.1.0)
 *
 * @author jingtian
 * @date 2024/4/15
 */
public class OBOracleBetween4000And4100SchemaAccessor extends OBOracleSchemaAccessor {
    public OBOracleBetween4000And4100SchemaAccessor(JdbcOperations jdbcOperations,
            OracleDataDictTableNames dataDictTableNames) {
        super(jdbcOperations, dataDictTableNames);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBORACLE_4_0_x);
    }
}
