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
package com.oceanbase.odc.plugin.schema.oracle;

import java.sql.Connection;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.oboracle.OBOracleColumnExtension;
import com.oceanbase.odc.plugin.schema.oracle.browser.DBSchemaAccessors;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

/**
 * @author gaoda.xy
 * @date 2024/4/21 21:14
 */
@Extension
public class OracleColumnExtension extends OBOracleColumnExtension {

    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBSchemaAccessors.create(JdbcOperationsUtil.getJdbcOperations(connection));
    }

}
