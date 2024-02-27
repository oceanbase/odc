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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.sql.split.SqlSplitter;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
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
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.BaseSqlScriptImportJob;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

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
            ObjectType type = ObjectType.valueOfName(object.getType());
            switch (type) {
                case TABLE:
                    objects = new OracleTableExtension().list(conn, object.getSchema());
                    break;
                case VIEW:
                    objects = new OracleViewExtension().list(conn, object.getSchema());
                    break;
                case FUNCTION:
                    objects = new OracleFunctionExtension().list(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                case PROCEDURE:
                    objects = new OracleProcedureExtension().list(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                case SEQUENCE:
                    objects = new OracleSequenceExtension().list(conn, object.getSchema());
                    break;
                case TRIGGER:
                    objects = new OracleTriggerExtension().list(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                case PACKAGE:
                    objects = new OraclePackageExtension().list(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                case PACKAGE_BODY:
                    objects = new OraclePackageExtension().listPackageBodies(conn, object.getSchema()).stream()
                            .map(obj -> DBObjectIdentity.of(obj.getSchemaName(), obj.getType(), obj.getName()))
                            .collect(Collectors.toList());
                    break;
                case SYNONYM:
                    objects = new OracleSynonymExtension().list(conn, object.getSchema(), DBSynonymType.COMMON);
                    break;
                case PUBLIC_SYNONYM:
                    objects = new OracleSynonymExtension().list(conn, object.getSchema(), DBSynonymType.PUBLIC);
                    break;
                case TYPE:
                    objects = new OracleTypeExtension().list(conn, object.getSchema()).stream()
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
    protected SqlStatementIterator getStmtIterator() throws IOException {
        String charset = transferConfig.getEncoding().getAlias();
        return SqlSplitter.iterator(input.openStream(), Charset.forName(charset), ";", false);
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
