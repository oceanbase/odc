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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

public class MySQLSqlScriptImportJob extends BaseSqlScriptImportJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    public MySQLSqlScriptImportJob(ObjectResult object, DataTransferConfig transferConfig, URL input,
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
    protected SqlStatementIterator getStmtIterator() throws IOException {
        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();
        return SqlCommentProcessor.iterator(input.openStream(), Charset.forName(charset),
                new SqlCommentProcessor(dialectType, true, true, true));
    }

    @Override
    protected List<String> getPreSqlsForSchema() throws SQLException {
        List<String> preSqls = new LinkedList<>();
        if (StringUtils.equalsIgnoreCase(object.getType(), "TABLE")) {
            preSqls.add(Constants.DISABLE_FK);
        }
        if (transferConfig.isReplaceSchemaWhenExists() && isObjectExists()) {
            preSqls.add(String.format(Constants.DROP_OBJECT_FORMAT, object.getType(),
                    StringUtils.quoteMysqlIdentifier(object.getName())));
            LOGGER.info("{} will be dropped.", object.getSummary());
        }
        return preSqls;
    }

    @Override
    protected List<String> getPreSqlsForData() {
        List<String> preSqls = new ArrayList<>();
        preSqls.add(Constants.DISABLE_FK);
        if (transferConfig.isTruncateTableBeforeImport()) {
            preSqls.add(String.format(Constants.TRUNCATE_FORMAT, getObject().getSchema(),
                    StringUtils.quoteMysqlIdentifier(getObject().getName())));
            LOGGER.info("{} will be truncated.", object.getSummary());
        }
        return preSqls;
    }

    @Override
    protected List<String> getPostSqlsForSchema() {
        if (StringUtils.equalsIgnoreCase(object.getType(), "TABLE")) {
            return Collections.singletonList(Constants.ENABLE_FK);
        }
        return null;
    }

    @Override
    protected List<String> getPostSqlsForData() {
        if (StringUtils.equalsIgnoreCase(object.getType(), "TABLE")) {
            return Collections.singletonList(Constants.ENABLE_FK);
        }
        return null;
    }

    @Override
    protected List<String> getPreSqlsForExternal() {
        return null;
    }

}
