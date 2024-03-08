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
package com.oceanbase.odc.plugin.task.oracle.datatransfer.job;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import com.oceanbase.odc.plugin.schema.oracle.OracleFunctionExtension;
import com.oceanbase.odc.plugin.schema.oracle.OraclePackageExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleProcedureExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleSequenceExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleSynonymExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleTableExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleTriggerExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleTypeExtension;
import com.oceanbase.odc.plugin.schema.oracle.OracleViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.BaseSchemaExportJob;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
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
        return String.format("DROP %s \"%s\";\n", ObjectType.valueOfName(object.getType()).getName(), object.getName());
    }

    @Override
    protected boolean isPlObject() {
        return PL_OBJECTS.contains(object.getType());
    }

    @Override
    protected String queryDdlForDBObject() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("BEGIN\n"
                    + "DBMS_METADATA.set_transform_param (DBMS_METADATA.session_transform, 'EMIT_SCHEMA', FALSE);\n"
                    + "END;");
            String ddl;
            ObjectType type = ObjectType.valueOfName(object.getType());
            switch (type) {
                case TABLE:
                    ddl = new OracleTableExtension().getDetail(conn, object.getSchema(), object.getName()).getDDL();
                    break;
                case VIEW:
                    ddl = new OracleViewExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    break;
                case FUNCTION:
                    ddl = new OracleFunctionExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    break;
                case PROCEDURE:
                    ddl = new OracleProcedureExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    break;
                case SEQUENCE:
                    ddl = new OracleSequenceExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    break;
                case TRIGGER:
                    ddl = new OracleTriggerExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    break;
                case PACKAGE:
                    ddl = new OraclePackageExtension().getDetail(conn, object.getSchema(), object.getName())
                            .getPackageHead().getBasicInfo().getDdl();
                    break;
                case PACKAGE_BODY:
                    ddl = new OraclePackageExtension().getDetail(conn, object.getSchema(), object.getName())
                            .getPackageBody().getBasicInfo().getDdl();
                    break;
                case SYNONYM:
                    ddl = new OracleSynonymExtension()
                            .getDetail(conn, object.getSchema(), object.getName(), DBSynonymType.COMMON).getDdl();
                    break;
                case PUBLIC_SYNONYM:
                    ddl = new OracleSynonymExtension()
                            .getDetail(conn, object.getSchema(), object.getName(), DBSynonymType.PUBLIC).getDdl();
                    break;
                case TYPE:
                    ddl = new OracleTypeExtension().getDetail(conn, object.getSchema(), object.getName()).getDdl();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            ddl = ddl.trim();
            if (!ddl.endsWith(";")) {
                ddl += ";";
            }
            return ddl;
        }
    }
}
