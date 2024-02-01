/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.plugin.task.oracle.datatransfer.job;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.BaseSqlScriptImportJob;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/1
 */
public class OracleSqlScriptImportJob extends BaseSqlScriptImportJob {

    public OracleSqlScriptImportJob(ObjectResult object, DataTransferConfig transferConfig, URL input,
            DataSource dataSource) {
        super(object, transferConfig, input, dataSource);
    }

    @Override
    protected boolean isObjectExists() throws SQLException {
        DBObjectIdentity target =
                DBObjectIdentity.of(object.getSchema(), DBObjectType.getEnumByName(object.getType()), object.getName());
        List<DBObjectIdentity> objects;
        try (Connection conn = dataSource.getConnection()) {
            switch (object.getType()) {
                // TODO: use OracleSchemaExtension
                case "TABLE":
                    objects = new MySQLTableExtension().list(conn, object.getSchema());
                    break;
                case "VIEW":
                    objects = new MySQLViewExtension().list(conn, object.getSchema());
                    break;
                case "FUNCTION":
                    objects = new MySQLFunctionExtension().list(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                case "PROCEDURE":
                    objects = new MySQLProcedureExtension().list(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        return CollectionUtils.containsAny(objects, target);
    }

    @Override
    protected List<String> getPreSqlsForSchema() {
        return Collections.singletonList("alter session set CURRENT_SCHEMA=" + transferConfig.getSchemaName());
    }

    @Override
    protected List<String> getPreSqlsForData() {
        return Collections.singletonList("alter session set CURRENT_SCHEMA=" + transferConfig.getSchemaName());
    }

    @Override
    protected List<String> getPostSqlsForSchema() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getPostSqlsForData() {
        return Collections.emptyList();
    }
}
