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
package com.oceanbase.odc.plugin.schema.oboracle;

import java.sql.Connection;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleProcedureTemplate;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleProcedureExtension extends OBMySQLProcedureExtension {
    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    @Override
    protected DBObjectOperator getOperator(Connection connection) {
        return new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    @Override
    protected DBObjectTemplate<DBProcedure> getTemplate() {
        return new OracleProcedureTemplate();
    }
}
