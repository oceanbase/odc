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
package com.oceanbase.odc.service.db;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.db.model.OdcDBVariable;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@SkipAuthorize("inside connect session")
public class DBRecyclebinSettingsService {

    private static final String SETTINGS_SCOPE = "global";
    private static final String ENABLED_VALUE = "ON";
    private static final String RECYCLEBIN_ENABLED_VARIABLE = "recyclebin";
    private static final String TRUNCATE_FLASHBACK_ENABLED_VARIABLE = "ob_enable_truncate_flashback";
    @Autowired
    private DBVariablesService variablesService;

    public RecyclebinSettings get(@NonNull ConnectionSession connectionSession) {
        List<OdcDBVariable> variables = variablesService.list(connectionSession, SETTINGS_SCOPE);
        Map<String, OdcDBVariable> key2Variable =
                variables.stream().collect(Collectors.toMap(OdcDBVariable::getKey, t -> t));
        RecyclebinSettings settings = new RecyclebinSettings();
        if (key2Variable.containsKey(RECYCLEBIN_ENABLED_VARIABLE)) {
            settings.setRecyclebinEnabled(StringUtils.equalsIgnoreCase(ENABLED_VALUE,
                    key2Variable.get(RECYCLEBIN_ENABLED_VARIABLE).getValue()));
        }
        if (key2Variable.containsKey(TRUNCATE_FLASHBACK_ENABLED_VARIABLE)) {
            settings.setTruncateFlashbackEnabled(StringUtils.equalsIgnoreCase(ENABLED_VALUE,
                    key2Variable.get(TRUNCATE_FLASHBACK_ENABLED_VARIABLE).getValue()));
        }
        settings.setObjectExpireTime(getExpireTime(connectionSession));
        return settings;
    }

    public RecyclebinSettings update(@NonNull List<ConnectionSession> sessions,
            @NonNull RecyclebinSettings settings) {
        for (ConnectionSession session : sessions) {
            update(session, settings);
        }
        log.info("Update recyclebin settings, settings={}, sessionCount={}", settings, sessions.size());
        return get(sessions.get(0));
    }

    public String getExpireTime(@NonNull ConnectionSession session) {
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        String sql = "show parameters like 'recyclebin_object_expire_time'";
        List<String> list = jdbcOperations.query(sql, (rs, rowNum) -> rs.getString(7));
        return CollectionUtils.isEmpty(list) ? "0s" : list.get(0);
    }

    private void update(ConnectionSession session, RecyclebinSettings settings) {
        PreConditions.validRequestState(
                Objects.nonNull(settings.getRecyclebinEnabled())
                        || Objects.nonNull(settings.getTruncateFlashbackEnabled()),
                ErrorCodes.BadRequest, null, "All recyclebin settings are null");
        SqlBuilder sqlBuilder = getBuilder(session);
        if (Objects.nonNull(settings.getRecyclebinEnabled())) {
            sqlBuilder.append("set session ")
                    .append(RECYCLEBIN_ENABLED_VARIABLE)
                    .append("=").value(settings.getRecyclebinEnabled() ? "ON" : "OFF")
                    .append(",global ")
                    .append(RECYCLEBIN_ENABLED_VARIABLE)
                    .append("=").value(settings.getRecyclebinEnabled() ? "ON" : "OFF");
        }
        if (Objects.nonNull(settings.getTruncateFlashbackEnabled())) {
            if (sqlBuilder.length() == 0) {
                sqlBuilder.append("set session ");
            } else {
                sqlBuilder.append(",session ");
            }
            sqlBuilder.append(TRUNCATE_FLASHBACK_ENABLED_VARIABLE)
                    .append("=").value(settings.getTruncateFlashbackEnabled() ? "ON" : "OFF")
                    .append(",global ")
                    .append(TRUNCATE_FLASHBACK_ENABLED_VARIABLE)
                    .append("=").value(settings.getTruncateFlashbackEnabled() ? "ON" : "OFF");
        }
        String sql = sqlBuilder.toString();
        session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(sql);
        log.info("Update session variable, sid={}, sql={}", session.getId(), sql);
    }

    private SqlBuilder getBuilder(ConnectionSession session) {
        if (session.getDialectType().isMysql()) {
            return new MySQLSqlBuilder();
        } else if (session.getDialectType().isOracle()) {
            return new OracleSqlBuilder();
        }
        throw new IllegalArgumentException("Unsupported dialect type, " + session.getDialectType());
    }

    @Data
    public static class UpdateRecyclebinSettingsReq {
        @NotEmpty
        private List<String> sessionIds;
        @NotNull
        private DBRecyclebinSettingsService.RecyclebinSettings settings;
    }

    /**
     * 回收站设置
     */
    @Data
    public static class RecyclebinSettings {
        /**
         * 是否启用回收站
         */
        private Boolean recyclebinEnabled;

        /**
         * 回收站是否支持 truncate table 闪回
         */
        private Boolean truncateFlashbackEnabled;

        /**
         * 回收站内对象保留时间，由 sys 租户控制，普通租户下为只读参数 <br>
         * 设置为 0s 时，表示永久保留。
         */
        @JsonProperty(access = Access.READ_ONLY)
        private String objectExpireTime;
    }

}
