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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.DataSourceManager;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.Resource;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class SqlDataImportJobImpl extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private final DataTransferConfig transferConfig;
    private final Resource resource;
    private final List<String> insertionBuffer = new LinkedList<>();
    private final int batchSize;
    private final AtomicLong failures = new AtomicLong(0);
    private volatile boolean stop;

    public SqlDataImportJobImpl(ObjectResult object, DataTransferConfig transferConfig, Resource resource) {
        super(object);
        this.transferConfig = transferConfig;
        this.resource = resource;
        // limit in [10, 5000] to avoid OOM
        this.batchSize = transferConfig.getBatchCommitNum() == null ? Constants.DEFAULT_BATCH_SIZE
                : Math.min(Math.max(transferConfig.getBatchCommitNum(), 10), 5000);
    }

    @Override
    public void run() throws Exception {
        List<String> preSqls = new ArrayList<>();
        preSqls.add(Constants.DISABLE_FK);
        if (transferConfig.isTruncateTableBeforeImport()) {
            preSqls.add(String.format(Constants.TRUNCATE_PATTERN, getObject().getSchema(), getObject().getName()));
        }
        executeWithoutResult(preSqls);

        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        try (SqlCommentProcessor.SqlStatementIterator iterator =
                SqlCommentProcessor.iterator(resource.openInputStream(), dialectType, true, true, true);
                Connection conn = ds.getConnection()) {
            while (!Thread.currentThread().isInterrupted() && !stop) {
                try {
                    offer(iterator);
                    poll(conn);
                    insertionBuffer.clear();
                } catch (Exception e) {
                    setStatus(Status.FAILURE);
                    throw e;
                }
            }
        } finally {
            executeWithoutResult(Collections.singletonList(Constants.ENABLE_FK));
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
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                stmt.execute(sql);
            }
        }
    }

    private void offer(Iterator<String> provider) {
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
            stop = true;
        }
    }

    private void poll(Connection conn) throws SQLException {
        if (insertionBuffer.isEmpty()) {
            return;
        }
        conn.setAutoCommit(false);
        try {
            batchInsert(conn);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void batchInsert(Connection conn) throws SQLException {
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
            singleInsert(conn);
        }
    }

    private void singleInsert(Connection conn) throws SQLException {
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
