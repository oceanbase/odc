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

import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
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
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class SqlScriptImportJob extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private final DataTransferConfig transferConfig;
    private final URL input;
    private final DataSource dataSource;
    private final AtomicLong failures = new AtomicLong(0);

    public SqlScriptImportJob(ObjectResult object, DataTransferConfig transferConfig, URL input,
            DataSource dataSource) {
        super(object);
        this.transferConfig = transferConfig;
        this.input = input;
        this.dataSource = dataSource;
    }

    @Override
    public void run() throws Exception {
        if (!transferConfig.isCompressed()) {
            // external sql script
            runExternalSqlScript();
            return;
        }
        int index = input.getFile().indexOf(Constants.DDL_SUFFIX);
        if (index > 0) {
            // schema file
            runSchemaScript();
        } else {
            // data file
            runDataScript();
        }
    }

    private void runExternalSqlScript() throws Exception {
        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();

        try (SqlCommentProcessor.SqlStatementIterator iterator = SqlCommentProcessor
                .iterator(input.openStream(), dialectType, true, true, true, Charset.forName(charset));
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            while (!isCanceled() && !Thread.currentThread().isInterrupted() && iterator.hasNext()) {
                String sql = iterator.next();
                try {
                    increaseTotal(1);
                    stmt.execute(sql);
                    increaseCount(1);
                } catch (Exception e) {
                    String errMsg =
                            String.format("Error occurred when executing sql: [%s], reason: %s", sql, e.getMessage());
                    LOGGER.warn(errMsg);
                    failures.getAndIncrement();
                }
            }
            if (failures.get() != 0L) {
                setStatus(Status.FAILURE);
                throw new RuntimeException(String.format("Sql record import task finished with some failed records. "
                        + "Number of failed records: %d", failures.get()));
            }
            setStatus(Status.SUCCESS);
        }
    }

    private void runSchemaScript() throws Exception {
        increaseTotal(1);
        if (!transferConfig.isReplaceSchemaWhenExists() && isObjectExists()) {
            LOGGER.info("Object {} already exists, skip it.", object.getSummary());
            increaseCount(1);
            setStatus(Status.SUCCESS);
            return;
        }

        List<String> preSqls = new LinkedList<>();
        if (StringUtils.equalsIgnoreCase(object.getType(), "TABLE")) {
            preSqls.add(Constants.DISABLE_FK);
        }
        if (transferConfig.isReplaceSchemaWhenExists()) {
            preSqls.add(String.format(Constants.DROP_OBJECT_FORMAT, object.getType(),
                    StringUtils.quoteMysqlIdentifier(object.getName())));
            LOGGER.info("{} will be dropped.", object.getSummary());
        }
        executeWithoutResult(preSqls);

        boolean firstLine = true;
        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();
        try (SqlCommentProcessor.SqlStatementIterator iterator = SqlCommentProcessor
                .iterator(input.openStream(), dialectType, true, true, true, Charset.forName(charset));
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            while (!Thread.currentThread().isInterrupted() && iterator.hasNext() && !isCanceled()) {
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
            if (StringUtils.equalsIgnoreCase(object.getType(), "TABLE")) {
                executeWithoutResult(Constants.ENABLE_FK);
            }
        }
        increaseCount(1);
        setStatus(Status.SUCCESS);
    }

    private void runDataScript() throws Exception {
        int batchSize = transferConfig.getBatchCommitNum() == null ? Constants.DEFAULT_BATCH_SIZE
                : Math.min(Math.max(transferConfig.getBatchCommitNum(), 10), 5000);
        List<String> preSqls = new ArrayList<>();
        preSqls.add(Constants.DISABLE_FK);
        if (transferConfig.isTruncateTableBeforeImport()) {
            preSqls.add(String.format(Constants.TRUNCATE_FORMAT, getObject().getSchema(),
                    StringUtils.quoteMysqlIdentifier(getObject().getName())));
            LOGGER.info("{} will be truncated.", object.getSummary());
        }
        executeWithoutResult(preSqls);

        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();
        try (SqlCommentProcessor.SqlStatementIterator iterator = SqlCommentProcessor
                .iterator(input.openStream(), dialectType, true, true, true, Charset.forName(charset));
                Connection conn = dataSource.getConnection()) {
            List<String> insertionBuffer = new LinkedList<>();
            while (!Thread.currentThread().isInterrupted() && !isCanceled()) {
                try {
                    offer(iterator, insertionBuffer, batchSize);
                    poll(conn, insertionBuffer);
                    insertionBuffer.clear();
                } catch (Exception e) {
                    setStatus(Status.FAILURE);
                    throw e;
                }
            }
        } finally {
            executeWithoutResult(Constants.ENABLE_FK);
        }

        if (failures.get() != 0L) {
            setStatus(Status.FAILURE);
            throw new RuntimeException(String.format("Sql record import task finished with some failed records. "
                    + "Number of failed records: %d", failures.get()));
        }
        setStatus(Status.SUCCESS);
    }


    private void executeWithoutResult(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void executeWithoutResult(List<String> sqls) throws SQLException {
        if (CollectionUtils.isEmpty(sqls)) {
            return;
        }
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                stmt.execute(sql);
            }
        }
    }

    private void offer(Iterator<String> provider, List<String> insertionBuffer, int batchSize) {
        for (int i = 0; i < batchSize && provider.hasNext(); i++) {
            String next = provider.next();
            if (next.equalsIgnoreCase(Constants.COMMIT_STMT)) {
                break;
            }
            insertionBuffer.add(next);
        }
        increaseTotal(insertionBuffer.size());
        if (!provider.hasNext()) {
            // EOF
            cancel();
        }
    }

    private void poll(Connection conn, List<String> insertionBuffer) throws SQLException {
        if (insertionBuffer.isEmpty()) {
            return;
        }
        conn.setAutoCommit(false);
        try {
            batchInsert(conn, insertionBuffer);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void batchInsert(Connection conn, List<String> insertionBuffer) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (String sql : insertionBuffer) {
                stmt.addBatch(sql);
            }
            int[] succeeds = stmt.executeBatch();
            conn.commit();
            increaseCount(Arrays.stream(succeeds).sum());
        } catch (SQLException e) {
            LOGGER.warn("rollback this batch, because: " + e.getMessage());
            conn.rollback();
            singleInsert(conn, insertionBuffer);
        }
    }

    private void singleInsert(Connection conn, List<String> insertionBuffer) throws SQLException {
        conn.setAutoCommit(true);
        try (Statement stmt = conn.createStatement()) {
            for (String sql : insertionBuffer) {
                try {
                    increaseCount(stmt.executeUpdate(sql));
                } catch (SQLException e) {
                    LOGGER.warn("Dirty record: [{}], reason: {}", sql, e.getMessage());
                    failures.getAndIncrement();

                    if (transferConfig.isStopWhenError()) {
                        throw e;
                    }
                }
            }
        }
    }

    private boolean isObjectExists() throws SQLException {
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

}
