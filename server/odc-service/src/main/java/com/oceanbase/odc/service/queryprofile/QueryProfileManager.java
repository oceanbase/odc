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
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.BYTES_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.CHANGE_TIME;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.DB_TIME;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.IO_READ_BYTES;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.OTHER_STATS;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.RESCAN_TIMES;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.ROWS_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.STATUS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.TimespanFormatUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.model.QueryStatus;
import com.oceanbase.odc.core.shared.model.SqlPlanGraph;
import com.oceanbase.odc.core.shared.model.SqlPlanMonitor;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.queryprofile.display.PlanGraph;
import com.oceanbase.odc.service.queryprofile.display.PlanGraphEdge;
import com.oceanbase.odc.service.queryprofile.display.PlanGraphOperator;
import com.oceanbase.odc.service.queryprofile.helper.PlanGraphMapper;
import com.oceanbase.odc.service.queryprofile.model.SqlProfile;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/19
 */
@Slf4j
@Component
public class QueryProfileManager {

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, r -> new Thread(r, "query-profile-" + r.hashCode()));

    public void submitProfile(ConnectionSession session, @NonNull String traceId) {
        executor.execute(() -> {
            if (ConnectionSessionUtil.getBinaryContentMetadata(session, traceId) == null) {
                SqlProfile profile = new SqlProfile(traceId, session);
                initiateProfile(profile);
                saveProfile(profile);
            }
        });
    }

    public SqlProfile getProfile(@NonNull String traceId, ConnectionSession session) throws IOException {
        BinaryContentMetaData metadata = ConnectionSessionUtil.getBinaryContentMetadata(session, traceId);
        if (metadata != null) {
            InputStream stream = ConnectionSessionUtil.getBinaryDataManager(session).read(metadata);
            SqlProfile profile =
                    JsonUtils.fromJson(StreamUtils.copyToString(stream, StandardCharsets.UTF_8), SqlProfile.class);
            profile.setSession(session);
            refreshProfile(profile);
            return profile;
        }
        SqlProfile profile = new SqlProfile(traceId, session);
        initiateProfile(profile);
        refreshProfile(profile);
        return profile;
    }

    public void initiateProfile(SqlProfile profile) {
        ConnectionSession session = profile.getSession();
        if (session.isExpired()) {
            log.warn("session is expired, profile with traceId={} will be thrown.", profile.getTraceId());
            return;
        }
        String traceId = profile.getTraceId();
        SqlPlanGraph graph = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                (StatementCallback<SqlPlanGraph>) stmt -> ConnectionPluginUtil
                        .getDiagnoseExtension(session.getConnectType().getDialectType())
                        .getSqlPlanGraphByTraceId(stmt.getConnection(), traceId));
        Verify.notNull(graph, "plan graph");
        profile.setGraph(PlanGraphMapper.toVO(graph));
    }

    public void refreshProfile(SqlProfile profile) {
        ConnectionSession session = profile.getSession();
        if (session.isExpired()) {
            log.warn("session is expired, profile with traceId={} will be thrown.", profile.getTraceId());
            return;
        }
        List<SqlPlanMonitor> spmStats = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                (StatementCallback<List<SqlPlanMonitor>>) stmt -> {
                    Map<String, String> statId2Name = OBUtils.querySPMStatNames(stmt, session.getConnectType());
                    return OBUtils.querySqlPlanMonitorStats(
                            stmt, profile.getTraceId(), session.getConnectType(), statId2Name);
                });
        replaceSPMStatsIntoProfile(spmStats, profile);
    }

    public void saveProfile(SqlProfile profile) {
        ConnectionSession session = profile.getSession();
        BinaryDataManager binaryDataManager = ConnectionSessionUtil.getBinaryDataManager(session);
        try {
            BinaryContentMetaData metaData = binaryDataManager.write(
                    new ByteArrayInputStream(JsonUtils.toJson(profile).getBytes()));
            ConnectionSessionUtil.setBinaryContentMetadata(session, profile.getTraceId(), metaData);
        } catch (IOException e) {
            log.warn("Failed to persist profile.", e);
        }
    }

    private void replaceSPMStatsIntoProfile(List<SqlPlanMonitor> records, SqlProfile profile) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        PlanGraph graph = profile.getGraph();
        records.sort(Comparator.comparingInt(stat -> Integer.parseInt(stat.getPlanLineId())));
        SqlPlanMonitor rootOperator = records.get(0);
        profile.setStartTime(rootOperator.getFirstRefreshTime());

        if (rootOperator.getLastRefreshTime() == null) {
            graph.putOverview(STATUS, QueryStatus.RUNNING.name());

            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getCurrentTime().toInstant());
            graph.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(totalTs));

            profile.setStatus(QueryStatus.RUNNING);
            profile.setDuration(totalTs.toNanos() / 1000);
        } else {
            graph.putOverview(STATUS, QueryStatus.FINISHED.name());

            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getLastRefreshTime().toInstant());
            graph.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(totalTs));

            profile.setEndTime(rootOperator.getLastRefreshTime());
            profile.setStatus(QueryStatus.FINISHED);
            profile.setDuration(totalTs.toNanos() / 1000);
        }

        graph.clearStatistics();
        for (SqlPlanMonitor stats : records) {
            PlanGraphOperator operator = graph.getOperator(stats.getPlanLineId());
            // overview
            if (stats.getLastChangeTime() != null) {
                operator.setStatus(QueryStatus.FINISHED);
                Duration changeTs = Duration.between(stats.getFirstChangeTime().toInstant(),
                        stats.getLastChangeTime().toInstant());
                operator.putOverview(CHANGE_TIME, TimespanFormatUtil.formatTimespan(changeTs));
            } else {
                if (stats.getFirstChangeTime() == null) {
                    operator.setStatus(QueryStatus.PREPARING);
                } else {
                    operator.setStatus(QueryStatus.RUNNING);
                }
                // if operator is interrupted, the last_change_time would also be null
                if (stats.getLastRefreshTime() != null) {
                    operator.setStatus(QueryStatus.FINISHED);
                }
            }
            Long dbTime = stats.getDbTime();
            if (operator.getDuration() != null) {
                dbTime = Math.max(dbTime, operator.getDuration());
            }
            operator.setDuration(dbTime);
            operator.putOverview(DB_TIME,
                    TimespanFormatUtil.formatTimespan(dbTime, TimeUnit.MICROSECONDS));
            // statistics
            operator.putStatistics(RESCAN_TIMES, stats.getStarts() + "");
            operator.putStatistics(IO_READ_BYTES, stats.getOtherstats().get(IO_READ_BYTES));
            operator.putStatistics(BYTES_IN_TOTAL, stats.getOtherstats().get(BYTES_IN_TOTAL));
            operator.putStatistics(ROWS_IN_TOTAL, stats.getOtherstats().get(ROWS_IN_TOTAL));
            graph.putStatistics(IO_READ_BYTES, stats.getOtherstats().get(IO_READ_BYTES));
            graph.putStatistics(BYTES_IN_TOTAL, stats.getOtherstats().get(BYTES_IN_TOTAL));
            graph.putStatistics(ROWS_IN_TOTAL, stats.getOtherstats().get(ROWS_IN_TOTAL));
            // attributes
            operator.putAttribute(OTHER_STATS, stats.getOtherstats());
            // output rows
            List<PlanGraphEdge> inEdges = operator.getInEdges();
            if (inEdges.size() == 1) {
                inEdges.get(0).setWeight(Float.valueOf(stats.getOutputRows()));
            }
        }
    }

}
