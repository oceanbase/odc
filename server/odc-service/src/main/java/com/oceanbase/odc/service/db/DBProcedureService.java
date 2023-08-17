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
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLProcedureTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleProcedureTemplate;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBProcedureService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBProcedure> list(ConnectionSession connectionSession, String dbName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listProcedures(dbName).stream().map(item -> {
            DBProcedure procedure = new DBProcedure();
            procedure.setProName(item.getName());
            procedure.setErrorMessage(item.getErrorMessage());
            procedure.setStatus(item.getStatus());
            return procedure;
        }).collect(Collectors.toList());
    }

    public DBProcedure detail(ConnectionSession connectionSession, String dbName, String proName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.getProcedure(dbName, proName);
    }

    public ResourceSql getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBProcedure resource) {
        DBObjectTemplate<DBProcedure> template;
        if (session.getDialectType().isMysql()) {
            template = new MySQLProcedureTemplate();
        } else if (session.getDialectType().isOracle()) {
            template = new OracleProcedureTemplate();
        } else {
            throw new UnsupportedOperationException("Unsupported dialect, " + session.getDialectType());
        }
        String ddl = template.generateCreateObjectTemplate(resource);
        return ResourceSql.ofSql(ddl);
    }

}
