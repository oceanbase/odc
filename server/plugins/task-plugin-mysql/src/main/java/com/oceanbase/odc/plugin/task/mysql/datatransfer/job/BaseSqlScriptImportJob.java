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
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/31
 */
public abstract class BaseSqlScriptImportJob extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    protected final DataTransferConfig transferConfig;
    protected final URL input;
    protected final DataSource dataSource;
    protected final AtomicLong failures = new AtomicLong(0);

    public BaseSqlScriptImportJob(ObjectResult object, DataTransferConfig transferConfig, URL input,
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

    abstract protected boolean isObjectExists() throws SQLException;

    abstract protected SqlStatementIterator getStmtIterator() throws IOException;

    abstract protected List<String> getPreSqlsForSchema();

    abstract protected List<String> getPreSqlsForData();

    abstract protected List<String> getPostSqlsForSchema();

    abstract protected List<String> getPostSqlsForData();

    private void runExternalSqlScript() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            SqlStatementIterator iterator = getStmtIterator();
            while (!isCanceled() && !Thread.currentThread().isInterrupted() && iterator.hasNext()) {
                String sql = iterator.next().getStr();
                if (StringUtils.isEmpty(sql.trim())) {
                    continue;
                }
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
                if (object.getTotal().get() % 100 == 0) {
                    LOGGER.info("Processed {} SQL statements.", object.getTotal().get());
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

        executeWithoutResult(getPreSqlsForSchema());

        boolean firstLine = true;
        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            SqlStatementIterator iterator = SqlCommentProcessor.iterator(input.openStream(), Charset.forName(charset),
                    new SqlCommentProcessor(dialectType, true, true, true));
            while (!Thread.currentThread().isInterrupted() && iterator.hasNext() && !isCanceled()) {
                String sql = iterator.next().getStr();
                if (StringUtils.isEmpty(sql.trim())
                        || (firstLine && (sql.startsWith("drop") || sql.startsWith("DROP")))) {
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
            executeWithoutResult(getPostSqlsForSchema());
        }
        increaseCount(1);
        setStatus(Status.SUCCESS);
    }

    private void runDataScript() throws Exception {
        int batchSize = transferConfig.getBatchCommitNum() == null ? Constants.DEFAULT_BATCH_SIZE
                : Math.min(Math.max(transferConfig.getBatchCommitNum(), 10), 5000);
        executeWithoutResult(getPreSqlsForData());

        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        String charset = transferConfig.getEncoding().getAlias();
        try (Connection conn = dataSource.getConnection()) {
            SqlStatementIterator iterator = SqlCommentProcessor.iterator(input.openStream(), Charset.forName(charset),
                    new SqlCommentProcessor(dialectType, true, true, true));
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
            executeWithoutResult(getPostSqlsForData());
        }

        if (failures.get() != 0L) {
            setStatus(Status.FAILURE);
            throw new RuntimeException(String.format("Sql record import task finished with some failed records. "
                    + "Number of failed records: %d", failures.get()));
        }
        setStatus(Status.SUCCESS);
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

    private void offer(SqlStatementIterator provider, List<String> insertionBuffer, int batchSize) {
        for (int i = 0; i < batchSize && provider.hasNext(); i++) {
            String next = provider.next().getStr();
            if (next.equalsIgnoreCase(Constants.COMMIT_STMT)) {
                break;
            }
            insertionBuffer.add(next);
            records++;
            bytes += next.getBytes(Charset.forName(transferConfig.getEncoding().getAlias())).length;
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

}
