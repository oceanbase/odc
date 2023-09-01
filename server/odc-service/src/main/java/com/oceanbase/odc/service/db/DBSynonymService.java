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
import com.oceanbase.odc.plugin.schema.api.SynonymExtensionPoint;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;

import lombok.NonNull;

/**
 * ODC同义词Service对象
 *
 * @author yh263208
 * @date 2020-12-19 20:47
 * @since ODC_release_2.4.0
 */
@Service
@SkipAuthorize("inside connect session")
public class DBSynonymService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBSynonym> list(ConnectionSession connectionSession, String dbName,
            DBSynonymType synonymType) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getSynonymExtensionPoint(connectionSession)
                        .list(con, dbName, synonymType))
                .stream().map(item -> {
                    DBSynonym synonym = new DBSynonym();
                    synonym.setSynonymName(item.getName());
                    return synonym;
                }).collect(Collectors.toList());
    }

    public String generateCreateSql(@NonNull ConnectionSession session, @NonNull DBSynonym synonym) {
        return session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getSynonymExtensionPoint(session)
                        .generateCreateDDL(synonym));
    }

    public DBSynonym detail(ConnectionSession connectionSession, DBSynonym synonym) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBSynonym>) con -> getSynonymExtensionPoint(connectionSession)
                        .getDetail(con, synonym.getOwner(), synonym.getSynonymName(), synonym.getSynonymType()));
    }

    private SynonymExtensionPoint getSynonymExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getSynonymExtension(session.getDialectType());
    }
}
