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
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.REMOTE_BYTES_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.REMOTE_IO_READ_BYTES;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.REMOTE_ROWS_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.RESCAN_TIMES;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.ROWS_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.STATUS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.google.common.collect.Comparators;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.TimespanFormatUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.model.QueryStatus;
import com.oceanbase.odc.core.shared.model.SqlPlanMonitor;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraph;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraphEdge;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraphOperator;
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

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, r -> new Thread(r, "query-profile-" + r.hashCode()));

    public void submitProfile(ConnectionSession session, @NonNull String traceId) {
        executor.execute(() -> {
            if (ConnectionSessionUtil.getBinaryContentMetadata(session, PROFILE_KEY_PREFIX + traceId) == null) {
                if (session.isExpired()) {
                    log.warn("session is expired, profile with traceId={} will be thrown.", traceId);
                    return;
                }
                SqlExplain profile = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                        (StatementCallback<SqlExplain>) stmt -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getConnectType().getDialectType())
                                .getQueryProfileByTraceId(stmt.getConnection(), traceId));
                BinaryDataManager binaryDataManager = ConnectionSessionUtil.getBinaryDataManager(session);
                try {
                    BinaryContentMetaData metaData = binaryDataManager.write(
                            new ByteArrayInputStream(JsonUtils.toJson(profile).getBytes()));
                    String key = PROFILE_KEY_PREFIX + traceId;
                    ConnectionSessionUtil.setBinaryContentMetadata(session, key, metaData);
                } catch (IOException e) {
                    log.warn("Failed to persist profile.", e);
                }
            }
        });
    }

    public SqlExplain getProfile(@NonNull String traceId, ConnectionSession session) throws IOException {
        BinaryContentMetaData metadata =
                ConnectionSessionUtil.getBinaryContentMetadata(session, PROFILE_KEY_PREFIX + traceId);
        if (metadata != null) {
            InputStream stream = ConnectionSessionUtil.getBinaryDataManager(session).read(metadata);
            SqlExplain profile =
                    JsonUtils.fromJson(StreamUtils.copyToString(stream, StandardCharsets.UTF_8), SqlExplain.class);
            refreshGraph(profile.getGraph(), session);
            return profile;
        }
        SqlExplain profile = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                (StatementCallback<SqlExplain>) stmt -> ConnectionPluginUtil
                        .getDiagnoseExtension(session.getConnectType().getDialectType())
                        .getQueryProfileByTraceId(stmt.getConnection(), traceId));
        Verify.notNull(profile, "profile graph");
        refreshGraph(profile.getGraph(), session);
        return profile;
    }

    public void refreshGraph(PlanGraph graph, ConnectionSession session) {
        if (session.isExpired()) {
            return;
        }
        List<SqlPlanMonitor> spmStats = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                (StatementCallback<List<SqlPlanMonitor>>) stmt -> {
                    Map<String, String> statId2Name = OBUtils.querySPMStatNames(stmt, session.getConnectType());
                    return OBUtils.querySqlPlanMonitorStats(
                            stmt, graph.getTraceId(), session.getConnectType(), statId2Name);
                });
        replaceSPMStatsIntoProfile(spmStats, graph);

        Map<String, List<String>> topNodes = new HashMap<>();
        graph.setTopNodes(topNodes);
        topNodes.put("duration", graph.getVertexes()
                .stream().collect(Comparators.greatest(5, Comparator.comparingLong(PlanGraphOperator::getDuration)))
                .stream().map(PlanGraphOperator::getGraphId).collect(Collectors.toList()));
    }

    private void replaceSPMStatsIntoProfile(List<SqlPlanMonitor> records, PlanGraph graph) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        records.sort(Comparator.comparingInt(stat -> Integer.parseInt(stat.getPlanLineId())));
        List<SqlPlanMonitor> mergedRecords = mergePlanMonitorRecords(records);

        SqlPlanMonitor rootOperator = mergedRecords.get(0);
        if (rootOperator.getLastRefreshTime() == null) {
            graph.putOverview(STATUS, QueryStatus.RUNNING.name());
            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getCurrentTime().toInstant());
            graph.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(totalTs));
            graph.setDuration(totalTs.toNanos() / 1000);
        } else {
            graph.putOverview(STATUS, QueryStatus.FINISHED.name());
            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getLastRefreshTime().toInstant());
            graph.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(totalTs));
            graph.setDuration(totalTs.toNanos() / 1000);
        }

        for (SqlPlanMonitor stats : mergedRecords) {
            PlanGraphOperator operator = graph.getOperator(stats.getPlanLineId());
            // overview
            if (stats.getLastRefreshTime() != null) {
                operator.setStatus(QueryStatus.FINISHED);
            } else {
                operator.setStatus(QueryStatus.RUNNING);
            }
            Long dbTime = stats.getDbTime();
            operator.setDuration(dbTime);
            operator.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(dbTime, TimeUnit.MICROSECONDS));
            if (stats.getLastChangeTime() != null) {
                Duration changeTs = Duration.between(stats.getFirstChangeTime().toInstant(),
                        stats.getLastChangeTime().toInstant());
                operator.putOverview(CHANGE_TIME, TimespanFormatUtil.formatTimespan(changeTs));
            } else {
                if (stats.getFirstChangeTime() == null) {
                    operator.setStatus(QueryStatus.PREPARING);
                } else {
                    Duration changeTs = Duration.between(stats.getFirstChangeTime().toInstant(),
                            stats.getCurrentTime().toInstant());
                    operator.putOverview(CHANGE_TIME, TimespanFormatUtil.formatTimespan(changeTs));
                }
            }
            // statistics
            operator.putStatistics(RESCAN_TIMES, stats.getStarts() + "");
            operator.putStatistics(IO_READ_BYTES, stats.getOtherstats().get(IO_READ_BYTES));
            operator.putStatistics(BYTES_IN_TOTAL, stats.getOtherstats().get(BYTES_IN_TOTAL));
            operator.putStatistics(ROWS_IN_TOTAL, stats.getOtherstats().get(ROWS_IN_TOTAL));
            operator.putStatistics(REMOTE_IO_READ_BYTES, stats.getOtherstats().get(REMOTE_IO_READ_BYTES));
            operator.putStatistics(REMOTE_BYTES_IN_TOTAL, stats.getOtherstats().get(REMOTE_BYTES_IN_TOTAL));
            operator.putStatistics(REMOTE_ROWS_IN_TOTAL, stats.getOtherstats().get(REMOTE_ROWS_IN_TOTAL));
            graph.addStatistics(IO_READ_BYTES, stats.getOtherstats().get(IO_READ_BYTES));
            graph.addStatistics(BYTES_IN_TOTAL, stats.getOtherstats().get(BYTES_IN_TOTAL));
            graph.addStatistics(ROWS_IN_TOTAL, stats.getOtherstats().get(ROWS_IN_TOTAL));
            graph.addStatistics(REMOTE_IO_READ_BYTES, stats.getOtherstats().get(REMOTE_IO_READ_BYTES));
            graph.addStatistics(REMOTE_BYTES_IN_TOTAL, stats.getOtherstats().get(REMOTE_BYTES_IN_TOTAL));
            graph.addStatistics(REMOTE_ROWS_IN_TOTAL, stats.getOtherstats().get(REMOTE_ROWS_IN_TOTAL));
            // attributes
            if (MapUtils.isNotEmpty(stats.getOtherstats())) {
                operator.putAttribute(OTHER_STATS, stats.getOtherstats().entrySet()
                        .stream().map(entry -> String.format("%s : %s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList()));
            }
            // output rows
            List<PlanGraphEdge> inEdges = operator.getInEdges();
            if (inEdges.size() == 1) {
                inEdges.get(0).setWeight(Float.valueOf(stats.getOutputRows()));
            }
        }
    }

    /**
     * When an operator is called on different machines, it will have multiple records in
     * sql_plan_monitor. We need to merge the data of these records based on the `plan_line_id`.
     */
    private List<SqlPlanMonitor> mergePlanMonitorRecords(List<SqlPlanMonitor> records) {
        String local = formatIpPort(records.get(0));
        Map<String, SqlPlanMonitor> planId2Records = new HashMap<>();
        for (SqlPlanMonitor record : records) {
            String planLineId = record.getPlanLineId();
            if (StringUtils.isEmpty(planLineId)) {
                continue;
            }
            if (!planId2Records.containsKey(planLineId)) {
                planId2Records.put(planLineId, record);
                continue;
            }
            SqlPlanMonitor realRecord = planId2Records.get(planLineId);

            String host = formatIpPort(record);
            if (StringUtils.equals(local, host)) {
                realRecord.getOtherstats().put(REMOTE_IO_READ_BYTES,
                        addStats(realRecord.getOtherstats().get(REMOTE_IO_READ_BYTES),
                                record.getOtherstats().get(IO_READ_BYTES)));
                realRecord.getOtherstats().put(REMOTE_BYTES_IN_TOTAL,
                        addStats(realRecord.getOtherstats().get(REMOTE_BYTES_IN_TOTAL),
                                record.getOtherstats().get(BYTES_IN_TOTAL)));
                realRecord.getOtherstats().put(REMOTE_ROWS_IN_TOTAL,
                        addStats(realRecord.getOtherstats().get(REMOTE_ROWS_IN_TOTAL),
                                record.getOtherstats().get(ROWS_IN_TOTAL)));
            } else {
                realRecord.getOtherstats().put(IO_READ_BYTES,
                        addStats(realRecord.getOtherstats().get(IO_READ_BYTES),
                                record.getOtherstats().get(IO_READ_BYTES)));
                realRecord.getOtherstats().put(BYTES_IN_TOTAL, addStats(realRecord.getOtherstats().get(BYTES_IN_TOTAL),
                        record.getOtherstats().get(BYTES_IN_TOTAL)));
                realRecord.getOtherstats().put(ROWS_IN_TOTAL,
                        addStats(realRecord.getOtherstats().get(ROWS_IN_TOTAL),
                                record.getOtherstats().get(ROWS_IN_TOTAL)));
            }
            realRecord.getOtherstats().put(RESCAN_TIMES,
                    addStats(realRecord.getOtherstats().get(RESCAN_TIMES), record.getOtherstats().get(RESCAN_TIMES)));
        }
        return planId2Records.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getKey())))
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    private String addStats(String a, String b) {
        long res = 0;
        if (StringUtils.isNumeric(a)) {
            res += Long.parseLong(a);
        }
        if (StringUtils.isNumeric(b)) {
            res += Long.parseLong(b);
        }
        return res == 0 ? null : res + "";
    }

    private String formatIpPort(SqlPlanMonitor stats) {
        return String.format("%s:%s", stats.getSvrIp(), stats.getSvrPort());
    }

}
