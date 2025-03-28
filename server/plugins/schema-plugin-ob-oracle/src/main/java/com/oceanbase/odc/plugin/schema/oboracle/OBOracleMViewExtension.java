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
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLMViewExtension;
import com.oceanbase.odc.plugin.schema.obmysql.parser.BaseOBGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.oboracle.parser.OBOracleGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/25 21:53
 * @since: 4.3.4
 */
@Extension
@Slf4j
public class OBOracleMViewExtension extends OBMySQLMViewExtension {

    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    @Override
    protected Statement parseStatement(String ddl) {
        return SqlParser.parseOracleStatement(ddl);
    }

    @Override
    protected DBObjectOperator getOperator(Connection connection) {
        return new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    @Override
    protected DBObjectTemplate<DBMaterializedView> getTemplate() {
        return DBBrowser.objectTemplate().mViewTemplate()
                .setType(DialectType.OB_ORACLE.getDBBrowserDialectTypeName()).create();
    }

    @Override
    protected BaseOBGetDBTableByParser getParser() {
        return new OBOracleGetDBTableByParser();
    }

}
