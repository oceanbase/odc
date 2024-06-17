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
package com.oceanbase.odc.service.queryprofile;

import static com.oceanbase.odc.core.session.ConnectionSessionConstants.BACKEND_DS_KEY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.plugin.connect.model.diagnose.SqlExplain;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/19
 */
@Slf4j
@Component
public class OBQueryProfileManager {
    private static final String PROFILE_KEY_PREFIX = "query-profile-";
    public static final String ENABLE_QUERY_PROFILE_VERSION = "4.2.4";

    @Autowired
    @Qualifier("queryProfileMonitorExecutor")
    private ThreadPoolTaskExecutor executor;

    public void submit(ConnectionSession session, @NonNull String traceId) {
        executor.execute(() -> {
            if (session.isExpired()
                    || ConnectionSessionUtil.getBinaryContentMetadata(session, PROFILE_KEY_PREFIX + traceId) != null) {
                return;
            }
            try {
                SqlExplain profile = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                        (ConnectionCallback<SqlExplain>) conn -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getConnectType().getDialectType())
                                .getQueryProfileByTraceId(conn, traceId));
                BinaryDataManager binaryDataManager = ConnectionSessionUtil.getBinaryDataManager(session);
                BinaryContentMetaData metaData = binaryDataManager.write(
                        new ByteArrayInputStream(JsonUtils.toJson(profile).getBytes()));
                String key = PROFILE_KEY_PREFIX + traceId;
                ConnectionSessionUtil.setBinaryContentMetadata(session, key, metaData);
            } catch (Exception e) {
                log.warn("Failed to cache profile.", e);
            }
        });
    }

    public SqlExplain getProfile(@NonNull String traceId, ConnectionSession session) {
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(session), ENABLE_QUERY_PROFILE_VERSION)) {
            throw new BadRequestException(ErrorCodes.ObQueryProfileNotSupported,
                    new Object[] {ENABLE_QUERY_PROFILE_VERSION},
                    ErrorCodes.ObQueryProfileNotSupported
                            .getLocalizedMessage(new Object[] {ENABLE_QUERY_PROFILE_VERSION}));
        }
        try {
            BinaryContentMetaData metadata =
                    ConnectionSessionUtil.getBinaryContentMetadata(session, PROFILE_KEY_PREFIX + traceId);
            if (metadata != null) {
                InputStream stream = ConnectionSessionUtil.getBinaryDataManager(session).read(metadata);
                return JsonUtils.fromJson(StreamUtils.copyToString(stream, StandardCharsets.UTF_8), SqlExplain.class);
            }
            return session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                    (StatementCallback<SqlExplain>) stmt -> ConnectionPluginUtil
                            .getDiagnoseExtension(session.getConnectType().getDialectType())
                            .getQueryProfileByTraceId(stmt.getConnection(), traceId));
        } catch (Exception e) {
            log.warn("Failed to get profile with OB trace_id={}.", traceId, e);
            throw new UnexpectedException(
                    String.format("Failed to get profile with OB trace_id=%s. Reason:%s", traceId, e.getMessage()));
        }
    }

}
