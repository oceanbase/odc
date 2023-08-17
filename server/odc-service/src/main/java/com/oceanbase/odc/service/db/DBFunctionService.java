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
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLFunctionTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleFunctionTemplate;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBFunctionService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBFunction> list(ConnectionSession connectionSession, String dbName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listFunctions(dbName).stream().map(item -> {
            DBFunction function = new DBFunction();
            function.setFunName(item.getName());
            function.setErrorMessage(item.getErrorMessage());
            function.setStatus(item.getStatus());
            return function;
        }).collect(Collectors.toList());
    }

    public DBFunction detail(ConnectionSession connectionSession, String dbName, String funName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.getFunction(dbName, funName);
    }

    public ResourceSql getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBFunction function) {
        DBObjectTemplate<DBFunction> template;
        if (session.getDialectType().isMysql()) {
            template = new MySQLFunctionTemplate();
        } else if (session.getDialectType().isOracle()) {
            template = new OracleFunctionTemplate();
        } else {
            throw new UnsupportedOperationException("Unsupported dialect, " + session.getDialectType());
        }
        String ddl = template.generateCreateObjectTemplate(function);
        return ResourceSql.ofSql(ddl);
    }

}
