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
package com.oceanbase.odc.plugin.schema.mysql.utils;

import java.sql.Connection;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLNoGreaterThan5740IndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLTableEditor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoGreaterThan5740SchemaAccessor;

/**
 * @author jingtian
 * @date 2024/1/2
 * @since ODC_release_4.2.4
 */
public class DBAccessorUtil {

    public static DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return new MySQLNoGreaterThan5740SchemaAccessor(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    public static DBTableEditor getTableEditor(Connection connection) {
        return new MySQLTableEditor(new MySQLNoGreaterThan5740IndexEditor(), new MySQLColumnEditor(),
                new MySQLConstraintEditor(),
                new MySQLDBTablePartitionEditor());
    }
}
