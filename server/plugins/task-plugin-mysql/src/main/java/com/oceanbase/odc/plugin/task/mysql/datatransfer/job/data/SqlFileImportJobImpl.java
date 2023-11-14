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

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.DataSourceManager;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.resource.Resource;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class SqlFileImportJobImpl extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private final DataTransferConfig transferConfig;
    private final Resource resource;
    private final AtomicInteger failures = new AtomicInteger(0);

    public SqlFileImportJobImpl(ObjectResult object, DataTransferConfig transferConfig, Resource resource) {
        super(object);
        this.transferConfig = transferConfig;
        this.resource = resource;
    }

    @Override
    public void run() throws Exception {
        DialectType dialectType = transferConfig.getConnectionInfo().getConnectType().getDialectType();
        DataSource ds = DataSourceManager.getInstance().get(transferConfig.getConnectionInfo());
        String charset = transferConfig.getEncoding().getAlias();

        try (SqlCommentProcessor.SqlStatementIterator iterator = SqlCommentProcessor
                .iterator(resource.openInputStream(), dialectType, true, true, true, Charset.forName(charset));
                Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement()) {
            while (!Thread.currentThread().isInterrupted() && iterator.hasNext()) {
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
}
