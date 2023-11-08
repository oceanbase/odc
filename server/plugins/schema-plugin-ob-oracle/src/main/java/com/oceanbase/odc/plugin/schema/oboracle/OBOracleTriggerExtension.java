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
import java.util.List;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.TriggerExtensionPoint;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTriggerTemplate;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleTriggerExtension implements TriggerExtensionPoint {

    @Override
    public List<DBPLObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return DBAccessorUtil.getSchemaAccessor(connection).listTriggers(schemaName);
    }

    @Override
    public DBTrigger getDetail(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String triggerName) {
        return DBAccessorUtil.getSchemaAccessor(connection).getTrigger(schemaName, triggerName);
    }

    @Override
    public void drop(@NonNull Connection connection, String schemaName, @NonNull String triggerName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.TRIGGER, null, triggerName);
    }

    @Override
    public void setEnable(@NonNull Connection connection, @NonNull String schemaName, @NonNull String triggerName,
            @NonNull boolean enable) {
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("ALTER TRIGGER ").identifier(schemaName).append(".").identifier(triggerName);
        if (enable) {
            sqlBuilder.append(" ENABLE");
        } else {
            sqlBuilder.append(" DISABLE");
        }
        JdbcOperationsUtil.getJdbcOperations(connection).execute(sqlBuilder.toString());
    }

    @Override
    public String generateUpdateTemplate(@NonNull DBTrigger oldTrigger, @NonNull DBTrigger newTrigger) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateCreateTemplate(@NonNull DBTrigger trigger) {
        DBObjectTemplate<DBTrigger> template = new OracleTriggerTemplate();
        return template.generateCreateObjectTemplate(trigger);
    }
}
