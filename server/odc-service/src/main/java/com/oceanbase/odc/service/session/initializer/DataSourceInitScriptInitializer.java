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

package com.oceanbase.odc.service.session.initializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DataSourceInitScriptInitializer}
 *
 * @author yh263208
 * @date 2023-09-12 17:31
 * @since ODC_release_4.2.2
 */
@Slf4j
public class DataSourceInitScriptInitializer implements ConnectionInitializer {

    private final ConnectionConfig connectionConfig;
    private final boolean eatException;

    public DataSourceInitScriptInitializer(ConnectionConfig connectionConfig) {
        this(connectionConfig, true);
    }

    public DataSourceInitScriptInitializer(@NonNull ConnectionConfig connectionConfig, boolean eatException) {
        this.eatException = eatException;
        this.connectionConfig = connectionConfig;
    }

    @Override
    public void init(Connection connection) throws SQLException {
        long start = System.currentTimeMillis();
        String initScript = connectionConfig.getSessionInitScript();
        if (StringUtils.isEmpty(initScript)) {
            return;
        }
        List<String> sqls = SqlCommentProcessor.removeSqlComments(
                initScript, ";", connectionConfig.getDialectType(), false).stream().map(OffsetString::getStr).collect(
                        Collectors.toList());
        if (CollectionUtils.isEmpty(sqls)) {
            return;
        }
        for (String sql : sqls) {
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(3);
                statement.execute(sql);
            } catch (SQLException e) {
                log.warn("Failed to execute init sql, sql={}", sql, e);
                if (!this.eatException) {
                    throw e;
                }
            }
        }
        log.info("Init connection by custom script succeed, sqlCount={}, cost={} millis",
                sqls.size(), System.currentTimeMillis() - start);
    }

}
