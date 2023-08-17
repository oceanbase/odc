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
package com.oceanbase.odc.service.db;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.model.OdcSqlExecuteResult;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.DBTriggerReq;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTriggerTemplate;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * 和trigger相关的service对象，用于封装相关的逻辑
 *
 * @author yh263208
 * @date 2020-12-04 15:10
 * @since ODC_release_2.4.0
 */
@Service
@SkipAuthorize("inside connect session")
public class DBTriggerService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBTrigger> list(ConnectionSession connectionSession, String dbName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listTriggers(dbName).stream().map(item -> {
            DBTrigger trigger = new DBTrigger();
            trigger.setTriggerName(item.getName());
            trigger.setErrorMessage(item.getErrorMessage());
            trigger.setEnable(item.getEnable());
            trigger.setStatus(item.getStatus());
            return trigger;
        }).collect(Collectors.toList());
    }

    public DBTrigger detail(ConnectionSession connectionSession, String schemaName, String triggerName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.getTrigger(schemaName, triggerName);
    }

    public DBTrigger alter(@NonNull ConnectionSession session, @NonNull DBTriggerReq unit) {
        dialectCheck(session);
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("ALTER TRIGGER ")
                .identifier(unit.getTriggerName());
        if (unit.isEnable()) {
            sqlBuilder.append(" ENABLE");
        } else {
            sqlBuilder.append(" DISABLE");
        }
        session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(sqlBuilder.toString());
        return unit;
    }

    public String generateCreateSql(@NonNull ConnectionSession session,
            @NonNull DBTriggerReq unit) {
        dialectCheck(session);
        DBObjectTemplate<DBTrigger> template = new OracleTriggerTemplate();
        return template.generateCreateObjectTemplate(unit);
    }

    public OdcSqlExecuteResult compile(@NonNull ConnectionSession session,
            @NonNull String triggerName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private void dialectCheck(@NonNull ConnectionSession session) {
        if (session.getDialectType() != DialectType.OB_ORACLE) {
            throw new UnsupportedOperationException("Trigger is not supported for " + session.getDialectType());
        }
    }

}
