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
package com.oceanbase.odc.service.db.browser;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.editor.GeneralSqlStatementBuilder;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/7/24 17:39
 * @Description: []
 */
public class GeneralSqlStatementBuilders {
    public static String drop(@NonNull ConnectType connectType, @NonNull DBObjectType objectType,
            String schemaName, @NonNull String objectName) {
        switch (connectType) {
            case OB_MYSQL:
            case CLOUD_OB_MYSQL:
                return GeneralSqlStatementBuilder.drop(new MySQLSqlBuilder(), objectType, schemaName, objectName);
            case CLOUD_OB_ORACLE:
            case OB_ORACLE:
                return GeneralSqlStatementBuilder.drop(new OracleSqlBuilder(), objectType, schemaName, objectName);
            default:
                throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }
}
