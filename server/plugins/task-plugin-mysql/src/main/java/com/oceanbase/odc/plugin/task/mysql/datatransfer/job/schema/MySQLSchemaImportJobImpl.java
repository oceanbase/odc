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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
import com.oceanbase.odc.plugin.schema.mysql.MySQLFunctionExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLProcedureExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.mysql.MySQLViewExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.DataSourceManager;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.Resource;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class MySQLSchemaImportJobImpl extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private final DataTransferConfig transferConfig;
    private final Resource resource;

    public MySQLSchemaImportJobImpl(ObjectResult object, DataTransferConfig transferConfig, Resource resource) {
        super(object);
        this.transferConfig = transferConfig;
        this.resource = resource;
    }

    @Override
    public void run() throws Exception {
        increaseTotal(1);
        if (!transferConfig.isReplaceSchemaWhenExists() && isObjectExists()) {
            LOGGER.info("Object {} already exists, skip it.", object.getSummary());
            increaseCount(1);
            setStatus(Status.SUCCESS);
            return;
        }

        List<String> preSqls = new LinkedList<>();
        if (StringUtils.equalsIgnoreCase(resource.getObjectType(), "TABLE")) {
            preSqls.add(Constants.DISABLE_FK);
        }
        if (transferConfig.isReplaceSchemaWhenExists()) {
            preSqls.add(String.format(Constants.DROP_OBJECT_FORMAT, object.getType(),
                    StringUtils.quoteMysqlIdentifier(object.getName())));
        }
        executeWithoutResult(preSqls);

        boolean firstLine = true;
        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        try (SqlCommentProcessor.SqlStatementIterator iterator =
                SqlCommentProcessor.iterator(resource.openInputStream(), dialectType, true, true, true, charset);
                Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement()) {
            while (!Thread.currentThread().isInterrupted() && iterator.hasNext()) {
                String sql = iterator.next();
                if (firstLine && sql.startsWith("drop") || sql.startsWith("DROP")) {
                    continue;
                }
                firstLine = false;

                try {
                    stmt.execute(sql);
                } catch (Exception e) {
                    setStatus(Status.FAILURE);
                    String errMsg =
                            String.format("Error occurred when executing sql: [%s], reason: %s", sql, e.getMessage());
                    LOGGER.warn(errMsg);
                    throw new RuntimeException(errMsg, e);
                }
            }
        } finally {
            if (StringUtils.equalsIgnoreCase(resource.getObjectType(), "TABLE")) {
                executeWithoutResult(Constants.ENABLE_FK);
            }
        }
        increaseCount(1);
        setStatus(Status.SUCCESS);
    }

    private void executeWithoutResult(String sql) throws SQLException {
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void executeWithoutResult(List<String> sqls) throws SQLException {
        if (CollectionUtils.isEmpty(sqls)) {
            return;
        }
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                stmt.execute(sql);
            }
        }
    }

    private boolean isObjectExists() throws SQLException {
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        DBObjectIdentity target =
                DBObjectIdentity.of(object.getSchema(), DBObjectType.getEnumByName(object.getType()), object.getName());
        List<DBObjectIdentity> objects;
        try (Connection conn = ds.getConnection()) {
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

}
