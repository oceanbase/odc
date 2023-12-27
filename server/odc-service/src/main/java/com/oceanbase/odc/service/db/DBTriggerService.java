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
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.plugin.schema.api.TriggerExtensionPoint;
import com.oceanbase.odc.service.db.model.DBTriggerReq;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;

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
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBPLObjectIdentity>>) con -> getTriggerExtensionPoint(
                        connectionSession).list(con, dbName))
                .stream()
                .map(item -> {
                    DBTrigger trigger = new DBTrigger();
                    trigger.setTriggerName(item.getName());
                    trigger.setErrorMessage(item.getErrorMessage());
                    trigger.setEnable(item.getEnable());
                    trigger.setStatus(item.getStatus());
                    return trigger;
                }).collect(Collectors.toList());
    }

    public DBTrigger detail(ConnectionSession connectionSession, String schemaName, String triggerName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBTrigger>) con -> getTriggerExtensionPoint(connectionSession)
                        .getDetail(con, schemaName, triggerName));
    }

    public DBTrigger alter(@NonNull ConnectionSession session, @NonNull DBTriggerReq unit) {
        String schemaName = ConnectionSessionUtil.getCurrentSchema(session);
        session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY).execute((ConnectionCallback<Void>) con -> {
                    getTriggerExtensionPoint(session).setEnable(con, schemaName, unit.getTriggerName(),
                            unit.isEnable());
                    return null;
                });
        return unit;
    }

    public String generateCreateSql(@NonNull ConnectionSession session,
            @NonNull DBTriggerReq unit) {
        return session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getTriggerExtensionPoint(session)
                        .generateCreateTemplate(unit));
    }

    private TriggerExtensionPoint getTriggerExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getTriggerExtension(session.getDialectType());
    }

}
