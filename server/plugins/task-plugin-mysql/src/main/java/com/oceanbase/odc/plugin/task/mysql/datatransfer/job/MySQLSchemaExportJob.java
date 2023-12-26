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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class MySQLSchemaExportJob extends AbstractJob {

    private final DataTransferConfig transferConfig;
    private final File workingDir;
    private final DataSource dataSource;

    public MySQLSchemaExportJob(ObjectResult object, DataTransferConfig transferConfig, File workingDir,
            DataSource dataSource) {
        super(object);
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
        this.dataSource = dataSource;
    }

    @Override
    public void run() throws Exception {
        increaseTotal(1);
        /*
         * build ddl
         */
        StringBuilder content = new StringBuilder();
        // 1. append DROP statement
        if (transferConfig.isWithDropDDL()) {
            content.append(assembleDropStatement());
        }
        // 2. append DELIMITER if it is a PL SQL
        if (isPlObject()) {
            content.append(Constants.PL_DELIMITER_STMT);
        }
        // 3. append CREATE statement
        content.append(queryDdlForDBObject());
        // 4. append '$$' if it is a PL SQL; The end of sql assembling
        if (isPlObject()) {
            content.append(Constants.LINE_BREAKER).append(Constants.DEFAULT_PL_DELIMITER);
        }
        /*
         * touch file
         */
        File output = Paths.get(workingDir.getPath(), "data", object.getSchema(), object.getType(),
                object.getName() + Constants.DDL_SUFFIX).toFile();
        if (!output.getParentFile().exists()) {
            FileUtils.forceMkdir(output.getParentFile());
        }
        FileUtils.touch(output);
        /*
         * overwrite content
         */
        FileUtils.write(output, content.toString(), transferConfig.getEncoding().getAlias(), false);
        object.setExportPaths(Collections.singletonList(output.toURI().toURL()));

        setStatus(Status.SUCCESS);
        increaseCount(1);
    }

    private boolean isPlObject() {
        return StringUtils.equalsIgnoreCase("FUNCTION", object.getType())
                || StringUtils.equalsIgnoreCase("PROCEDURE", object.getType());
    }

    private String assembleDropStatement() {
        return String.format(Constants.DROP_OBJECT_FORMAT, object.getType(),
                StringUtils.quoteMysqlIdentifier(object.getName()));
    }

    private String queryDdlForDBObject() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            switch (object.getType()) {
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
