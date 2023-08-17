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
package com.oceanbase.odc.service.session;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.session.model.SessionSettings;

/**
 * Transaction service object, used to config some settings
 *
 * @author yh263208
 * @date 2021-06-08 11:32
 * @since ODC_release_2.4.2
 */
@Service
@Validated
@SkipAuthorize("inside connect session")
public class SessionSettingsService {

    @Autowired
    private SessionProperties sessionProperties;

    public SessionSettings getSessionSettings(@NotNull ConnectionSession session) {
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        return jdbcOperations.query("show session variables like 'autocommit'", rs -> {
            SessionSettings settings = new SessionSettings();
            if (rs.next()) {
                settings.setAutocommit("on".equalsIgnoreCase(rs.getString(2)));
            }
            settings.setObVersion(ConnectionSessionUtil.getVersion(session));
            settings.setDelimiter(ConnectionSessionUtil.getSqlCommentProcessor(session).getDelimiter());
            settings.setQueryLimit(ConnectionSessionUtil.getQueryLimit(session));
            return settings;
        });
    }

    public SessionSettings setSessionSettings(@NotNull ConnectionSession session,
            @NotNull @Valid SessionSettings settings) {
        if (sessionProperties.getResultSetMaxRows() >= 0) {
            PreConditions.lessThanOrEqualTo("queryLimit", LimitMetric.TRANSACTION_QUERY_LIMIT,
                    settings.getQueryLimit(), sessionProperties.getResultSetMaxRows());
        }
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        String sql = "show session variables like 'autocommit'";
        List<String> lines = jdbcOperations.query(sql, (rs, rowNum) -> rs.getString(2));
        boolean commitFlag = false;
        if (CollectionUtils.isNotEmpty(lines)) {
            String autocommit = lines.get(0);
            commitFlag = "on".equalsIgnoreCase(autocommit);
        }
        if (commitFlag != settings.getAutocommit()) {
            sql = String.format("set session autocommit='%s'", settings.getAutocommit() ? "ON" : "OFF");
            jdbcOperations.execute(sql);
        }
        SqlCommentProcessor processor = ConnectionSessionUtil.getSqlCommentProcessor(session);
        if (!settings.getDelimiter().equals(processor.getDelimiter())) {
            processor.setDelimiter(settings.getDelimiter());
        }
        Integer queryLimit = ConnectionSessionUtil.getQueryLimit(session);
        if (!Objects.equals(settings.getQueryLimit(), queryLimit)) {
            ConnectionSessionUtil.setQueryLimit(session, settings.getQueryLimit());
        }
        return settings;
    }

}
