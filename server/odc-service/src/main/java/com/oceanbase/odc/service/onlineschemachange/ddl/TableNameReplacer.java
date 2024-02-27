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
package com.oceanbase.odc.service.onlineschemachange.ddl;

import java.util.List;

import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;

public interface TableNameReplacer {

    ReplaceResult replaceCreateStmt(String originCreateStmt, String newTableName);

    ReplaceResult replaceAlterStmt(String originAlterStmt, String newTableName);

    ReplaceResult replaceStmtValue(OnlineSchemaChangeSqlType sqlType, String originSql,
            List<ReplaceElement> replaceElements);

    String replaceCreateIndexStmt(String originCreateIndexStmt, String newTableName);

}
