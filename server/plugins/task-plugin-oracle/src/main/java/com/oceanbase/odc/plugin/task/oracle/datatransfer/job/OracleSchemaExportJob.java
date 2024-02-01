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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.BaseSchemaExportJob;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/1
 */
public class OracleSchemaExportJob extends BaseSchemaExportJob {
    private static final Set<String> PL_OBJECTS = new HashSet<>();

    static {
        PL_OBJECTS.add("FUNCTION");
        PL_OBJECTS.add("PROCEDURE");
        PL_OBJECTS.add("TRIGGER");
        PL_OBJECTS.add("TYPE");
        PL_OBJECTS.add("TYP_BODY");
        PL_OBJECTS.add("PACKAGE");
        PL_OBJECTS.add("PACKAGE_BODY");
    }

    public OracleSchemaExportJob(ObjectResult object, DataTransferConfig transferConfig, File workingDir,
            DataSource dataSource) {
        super(object, transferConfig, workingDir, dataSource);
    }

    @Override
    protected String getDropStatement() {
        return String.format("DROP %s %s", ObjectType.valueOfName(object.getType()).getName(), object.getName());
    }

    @Override
    protected boolean isPlObject() {
        return PL_OBJECTS.contains(object.getType());
    }

    @Override
    protected String queryDdlForDBObject() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            switch (object.getType()) {
                // TODO: use OracleSchemaExtension
                case "TABLE":
                    String ddl =
                            new MySQLTableExtension().getDetail(conn, object.getSchema(), object.getName()).getDDL();
                    if (!ddl.endsWith(";")) {
                        ddl += ";";
                    }
                    return ddl;
                case "VIEW":
                    ddl = new MySQLViewExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    if (!ddl.endsWith(";")) {
                        ddl += ";";
                    }
                    return ddl;
                case "FUNCTION":
                    return new MySQLFunctionExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                case "PROCEDURE":
                    return new MySQLProcedureExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
