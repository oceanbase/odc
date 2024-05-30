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
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.IO_READ_BYTES;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.MEMSTORE_ROWS_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.OTHER_STATS;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.OUTPUT_ROWS;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.SSSTORE_ROWS_IN_TOTAL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.START_TIMES;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.WORKAREA_MAX_MEN;
import static com.oceanbase.odc.service.queryprofile.model.ProfileConstants.WORKAREA_MAX_TEMPSEG;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.CHANGE_TIME;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.DB_TIME;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.IS_HIT_PLAN_CACHE;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.PARALLEL;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.PLAN_TYPE;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.PROCESS_NAME;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.QUEUE_TIME;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.SKEWNESS;
import static com.oceanbase.odc.service.queryprofile.model.ProfileOverviewKey.STATUS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.google.common.collect.Comparators;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.TimespanFormatUtil;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.QueryStatus;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
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

    public void submit(ConnectionSession session, @NonNull String traceId, Locale locale) {
        executor.execute(() -> {
            if (session.isExpired()
                    || ConnectionSessionUtil.getBinaryContentMetadata(session, PROFILE_KEY_PREFIX + traceId) != null) {
                return;
            }
            SqlExplain profile = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                    (ConnectionCallback<SqlExplain>) conn -> ConnectionPluginUtil
                            .getDiagnoseExtension(session.getConnectType().getDialectType())
                            .getQueryProfileByTraceId(conn, traceId));
            BinaryDataManager binaryDataManager = ConnectionSessionUtil.getBinaryDataManager(session);
            LocaleContextHolder.setLocale(locale);
            try {
                refreshProfile(profile.getGraph(), session);
                BinaryContentMetaData metaData = binaryDataManager.write(
                        new ByteArrayInputStream(JsonUtils.toJson(profile).getBytes()));
                String key = PROFILE_KEY_PREFIX + traceId;
                ConnectionSessionUtil.setBinaryContentMetadata(session, key, metaData);
            } catch (IOException e) {
                log.warn("Failed to cache profile.", e);
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
        });
    }

    public SqlExplain getProfile(@NonNull String traceId, ConnectionSession session) throws IOException {
        if (session.isExpired()) {
            throw new BadRequestException(ErrorCodes.ConnectionReset, null,
                    ErrorCodes.ConnectionReset.getLocalizedMessage(null));
        }
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(session), "4.2.0")) {
            throw new BadRequestException(ErrorCodes.ObQueryProfileNotSupported, null,
                    ErrorCodes.ObQueryProfileNotSupported.getLocalizedMessage(null));
        }
        try {
            BinaryContentMetaData metadata =
                    ConnectionSessionUtil.getBinaryContentMetadata(session, PROFILE_KEY_PREFIX + traceId);
            if (metadata != null) {
                InputStream stream = ConnectionSessionUtil.getBinaryDataManager(session).read(metadata);
                return JsonUtils.fromJson(StreamUtils.copyToString(stream, StandardCharsets.UTF_8), SqlExplain.class);
            }
            SqlExplain profile = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                    (StatementCallback<SqlExplain>) stmt -> ConnectionPluginUtil
                            .getDiagnoseExtension(session.getConnectType().getDialectType())
                            .getQueryProfileByTraceId(stmt.getConnection(), traceId));
            Verify.notNull(profile, "profile graph");
            refreshProfile(profile.getGraph(), session);
            return profile;
        } catch (Exception e) {
            log.warn("Failed to get profile with OB trace_id={}.", traceId, e);
            throw new UnexpectedException(
                    String.format("Failed to get profile with OB trace_id=%s. Reason:%s", traceId, e.getMessage()));
        }
    }

    private void refreshProfile(PlanGraph graph, ConnectionSession session) {
        String traceId = graph.getTraceId();
        try {
            List<SqlPlanMonitor> spmStats = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                    (StatementCallback<List<SqlPlanMonitor>>) stmt -> {
                        Map<String, String> statId2Name = OBUtils.querySPMStatNames(stmt, session.getConnectType());
                        return OBUtils.querySqlPlanMonitorStats(
                                stmt, traceId, session.getConnectType(), statId2Name);
                    });
            replaceSPMStatsIntoProfile(spmStats, graph);
        } catch (Exception e) {
            log.warn("Failed to get runtime statistics with OB trace_id={}.", traceId, e);
        }

        try {
            SqlExecDetail execDetail = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute(
                    (ConnectionCallback<SqlExecDetail>) conn -> ConnectionPluginUtil
                            .getDiagnoseExtension(session.getConnectType().getDialectType())
                            .getExecutionDetailById(conn, traceId));
            Verify.notNull(execDetail, "exec detail");
            graph.putOverview(QUEUE_TIME.getLocalizedMessage(),
                    TimespanFormatUtil.formatTimespan(execDetail.getQueueTime(), TimeUnit.MICROSECONDS));
            graph.putOverview(PLAN_TYPE.getLocalizedMessage(), execDetail.getPlanType());
            graph.putOverview(IS_HIT_PLAN_CACHE.getLocalizedMessage(), execDetail.isHitPlanCache() + "");
            if (graph.getDuration() == 0) {
                graph.setDuration(execDetail.getExecTime());
                graph.putOverview(DB_TIME.getLocalizedMessage(),
                        TimespanFormatUtil.formatTimespan(execDetail.getExecTime(), TimeUnit.MICROSECONDS));
            }
        } catch (Exception e) {
            log.warn("Failed to query sql audit with OB trace_id={}.", traceId, e);
        }

        Map<String, List<String>> topNodes = new HashMap<>();
        graph.setTopNodes(topNodes);
        topNodes.put("duration", graph.getVertexes()
                .stream().filter(o -> o.getDuration() != null)
                .collect(Comparators.greatest(5, Comparator.comparingLong(PlanGraphOperator::getDuration)))
                .stream().map(PlanGraphOperator::getGraphId).collect(Collectors.toList()));
    }

    private void replaceSPMStatsIntoProfile(List<SqlPlanMonitor> records, PlanGraph graph) {
        boolean needFilter = false;
        boolean rootOperatorExists = false;
        Timestamp startTs = new Timestamp(0);
        for (SqlPlanMonitor spm : records) {
            if (!"0".equals(spm.getPlanLineId())) {
                continue;
            }
            if (rootOperatorExists) {
                needFilter = true;
            } else {
                rootOperatorExists = true;
            }
            Verify.notNull(spm.getFirstRefreshTime(), "firstRefreshTime");
            if (startTs.compareTo(spm.getFirstRefreshTime()) < 1) {
                startTs = spm.getFirstRefreshTime();
            }
        }

        Map<String, List<SqlPlanMonitor>> planLineId2Spm = new TreeMap<>(Comparator.comparingInt(Integer::parseInt));
        for (SqlPlanMonitor spm : records) {
            if (!needFilter || spm.getFirstRefreshTime() == null || startTs.compareTo(spm.getFirstRefreshTime()) < 1) {
                List<SqlPlanMonitor> list = planLineId2Spm.computeIfAbsent(spm.getPlanLineId(), s -> new ArrayList<>());
                list.add(spm);
            }
        }

        if (planLineId2Spm.get("0").size() != 1) {
            log.warn("There are more than one root operator in v$sql_plan_monitor with trace_id={}.",
                    graph.getTraceId());
        }
        SqlPlanMonitor rootOperator = planLineId2Spm.get("0").get(0);
        if (rootOperator.getLastRefreshTime() == null) {
            graph.putOverview(STATUS.getLocalizedMessage(), QueryStatus.RUNNING.name());
            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getCurrentTime().toInstant());
            graph.putOverview(DB_TIME.getLocalizedMessage(), TimespanFormatUtil.formatTimespan(totalTs));
            graph.setDuration(totalTs.toNanos() / 1000);
        } else {
            graph.putOverview(STATUS.getLocalizedMessage(), QueryStatus.FINISHED.name());
            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getLastRefreshTime().toInstant());
            graph.putOverview(DB_TIME.getLocalizedMessage(), TimespanFormatUtil.formatTimespan(totalTs));
            graph.setDuration(totalTs.toNanos() / 1000);
        }

        for (List<SqlPlanMonitor> spms : planLineId2Spm.values()) {
            PlanGraphOperator operator = graph.getOperator(spms.get(0).getPlanLineId());
            try {
                if (spms.size() == 1) {
                    SqlPlanMonitor spm = spms.get(0);
                    setRuntimeStatistics(spm, operator, graph);
                } else {
                    mergeSPMRecords(spms, operator, graph);
                }
            } catch (Exception e) {
                log.warn("Failed to set runtime statistics with OB trace_id={} and plan_line_id={}", graph.getTraceId(),
                        spms.get(0).getPlanLineId());
            }
        }
    }

    private void mergeSPMRecords(List<SqlPlanMonitor> spms, PlanGraphOperator operator, PlanGraph graph) {
        List<PlanGraphOperator> subOperators = new ArrayList<>();
        long totalTs = 0;
        long starts = 0;
        float outputs = 0;
        long maxMem = 0;
        long maxDisk = 0;
        long ioBytes = 0;
        long ssstoreBytes = 0;
        long ssstoreRows = 0;
        long memstoreRows = 0;
        long maxChangeTs = -1;
        long minChangeTs = Integer.MAX_VALUE;

        operator.putOverview(STATUS.getLocalizedMessage(), QueryStatus.FINISHED.name());
        for (SqlPlanMonitor spm : spms) {
            PlanGraphOperator child = new PlanGraphOperator();
            if (spm.getLastRefreshTime() == null) {
                child.putOverview(STATUS.getLocalizedMessage(), QueryStatus.RUNNING.name());
                operator.putOverview(STATUS.getLocalizedMessage(), QueryStatus.RUNNING.name());
            } else {
                child.putOverview(STATUS.getLocalizedMessage(), QueryStatus.FINISHED.name());
            }
            child.putOverview(PROCESS_NAME.getLocalizedMessage(), spm.getProcessName());
            totalTs += spm.getDbTime();
            String dbTime = TimespanFormatUtil.formatTimespan(spm.getDbTime(), TimeUnit.MICROSECONDS);
            child.putOverview(DB_TIME.getLocalizedMessage(), dbTime);
            child.setTitle(String.format("%s(%s)", spm.getSvrIp(), spm.getProcessName()));
            child.setName(operator.getName());
            child.setDuration(spm.getDbTime());
            try {
                if (spm.getLastChangeTime() != null) {
                    Duration changeTime = Duration.between(spm.getFirstChangeTime().toInstant(),
                            spm.getLastChangeTime().toInstant());
                    child.putOverview(CHANGE_TIME.getLocalizedMessage(), TimespanFormatUtil.formatTimespan(changeTime));
                    long changeTs = changeTime.toNanos() / 1000;
                    if (changeTs != 0) {
                        maxChangeTs = Math.max(changeTs, maxChangeTs);
                        minChangeTs = Math.min(changeTs, minChangeTs);
                    }
                } else {
                    if (spm.getFirstChangeTime() == null) {
                        child.setStatus(QueryStatus.PREPARING);
                    } else {
                        Duration changeTime = Duration.between(spm.getFirstChangeTime().toInstant(),
                                spm.getCurrentTime().toInstant());
                        child.putOverview(CHANGE_TIME.getLocalizedMessage(),
                                TimespanFormatUtil.formatTimespan(changeTime));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to set change time, last_change_time={}, first_change_time={} ",
                        spm.getLastChangeTime(), spm.getFirstChangeTime());
            }
            outputs += spm.getOutputRows();
            child.putStatistics(OUTPUT_ROWS, spm.getOutputRows() + "");
            if (spm.getWorkareaMaxMem() != 0) {
                child.putStatistics(WORKAREA_MAX_MEN, spm.getWorkareaMaxMem() + "");
                maxMem = Math.max(spm.getWorkareaMaxMem(), maxMem);
            }
            if (spm.getWorkareaMaxTempSeg() != 0) {
                child.putStatistics(WORKAREA_MAX_TEMPSEG, spm.getWorkareaMaxTempSeg() + "");
                maxDisk = Math.max(spm.getWorkareaMaxTempSeg(), maxDisk);
            }
            if (spm.getStarts() != 0) {
                child.putStatistics(START_TIMES, spm.getStarts() + "");
                starts += spm.getStarts();
            }
            Map<String, String> otherstats = spm.getOtherstats();
            if (MapUtils.isNotEmpty(otherstats)) {
                child.putAttribute(OTHER_STATS, otherstats.entrySet()
                        .stream().map(entry -> String.format("%s : %s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList()));
            }
            if (otherstats.containsKey(IO_READ_BYTES)) {
                child.putStatistics(IO_READ_BYTES, otherstats.get(IO_READ_BYTES));
                ioBytes += Long.parseLong(otherstats.get(IO_READ_BYTES));
            }
            if (otherstats.containsKey(BYTES_IN_TOTAL)) {
                child.putStatistics(BYTES_IN_TOTAL, otherstats.get(BYTES_IN_TOTAL));
                ssstoreBytes += Long.parseLong(otherstats.get(BYTES_IN_TOTAL));
            }
            if (otherstats.containsKey(SSSTORE_ROWS_IN_TOTAL)) {
                child.putStatistics(SSSTORE_ROWS_IN_TOTAL, otherstats.get(SSSTORE_ROWS_IN_TOTAL));
                ssstoreRows += Long.parseLong(otherstats.get(SSSTORE_ROWS_IN_TOTAL));
            }
            if (otherstats.containsKey(MEMSTORE_ROWS_IN_TOTAL)) {
                child.putStatistics(MEMSTORE_ROWS_IN_TOTAL, otherstats.get(MEMSTORE_ROWS_IN_TOTAL));
                memstoreRows += Long.parseLong(otherstats.get(MEMSTORE_ROWS_IN_TOTAL));
            }
            subOperators.add(child);
        }

        if (ioBytes > 0 || ssstoreBytes > 0 || ssstoreRows > 0 || memstoreRows > 0) {
            operator.putStatistics(IO_READ_BYTES, ioBytes + "");
            operator.putStatistics(BYTES_IN_TOTAL, ssstoreBytes + "");
            operator.putStatistics(SSSTORE_ROWS_IN_TOTAL, ssstoreRows + "");
            operator.putStatistics(MEMSTORE_ROWS_IN_TOTAL, memstoreRows + "");
            graph.addStatistics(IO_READ_BYTES, ioBytes + "");
            graph.addStatistics(BYTES_IN_TOTAL, ssstoreBytes + "");
            graph.addStatistics(SSSTORE_ROWS_IN_TOTAL, ssstoreRows + "");
            graph.addStatistics(MEMSTORE_ROWS_IN_TOTAL, memstoreRows + "");
        }
        if (maxMem > 0 || maxDisk > 0) {
            operator.putStatistics(WORKAREA_MAX_MEN, maxMem + "");
            operator.putStatistics(WORKAREA_MAX_TEMPSEG, maxDisk + "");
        }
        if (starts > 0) {
            operator.putStatistics(START_TIMES, starts + "");
        }
        operator.putStatistics(OUTPUT_ROWS, outputs + "");

        long avgTs = totalTs / spms.size();
        operator.setDuration(avgTs);
        operator.putOverview(DB_TIME.getLocalizedMessage(),
                TimespanFormatUtil.formatTimespan(avgTs, TimeUnit.MICROSECONDS));

        operator.putOverview(PARALLEL.getLocalizedMessage(), spms.size() + "");

        if (maxChangeTs > 0) {
            operator.putOverview(SKEWNESS.getLocalizedMessage(),
                    String.format("%.2f", (maxChangeTs - minChangeTs) * 1f / maxChangeTs));
        }

        // output rows
        List<PlanGraphEdge> inEdges = operator.getInEdges();
        if (inEdges != null && inEdges.size() == 1) {
            inEdges.get(0).setWeight(outputs);
        }

        subOperators.sort(Comparator.comparingLong(PlanGraphOperator::getDuration).reversed());
        Map<String, PlanGraphOperator> map = new LinkedHashMap<>();
        for (PlanGraphOperator o : subOperators) {
            map.put(o.getTitle(), o);
        }
        operator.setSubNodes(map);
    }

    private void setRuntimeStatistics(SqlPlanMonitor stats, PlanGraphOperator operator, PlanGraph graph) {
        // overview
        if (stats.getLastRefreshTime() != null) {
            operator.setStatus(QueryStatus.FINISHED);
            operator.putOverview(STATUS.getLocalizedMessage(), QueryStatus.FINISHED.name());
        } else {
            operator.setStatus(QueryStatus.RUNNING);
            operator.putOverview(STATUS.getLocalizedMessage(), QueryStatus.RUNNING.name());
        }
        Long dbTime = stats.getDbTime();
        operator.setDuration(dbTime);
        operator.putOverview(DB_TIME.getLocalizedMessage(),
                TimespanFormatUtil.formatTimespan(dbTime, TimeUnit.MICROSECONDS));
        try {
            if (stats.getLastChangeTime() != null) {
                Duration changeTime = Duration.between(stats.getFirstChangeTime().toInstant(),
                        stats.getLastChangeTime().toInstant());
                operator.putOverview(CHANGE_TIME.getLocalizedMessage(), TimespanFormatUtil.formatTimespan(changeTime));
            } else {
                if (stats.getFirstChangeTime() == null) {
                    operator.setStatus(QueryStatus.PREPARING);
                } else {
                    Duration changeTime = Duration.between(stats.getFirstChangeTime().toInstant(),
                            stats.getCurrentTime().toInstant());
                    operator.putOverview(CHANGE_TIME.getLocalizedMessage(),
                            TimespanFormatUtil.formatTimespan(changeTime));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to set change time, last_change_time={}, first_change_time={} ",
                    stats.getLastChangeTime(), stats.getFirstChangeTime());
        }
        // statistics
        if (stats.getWorkareaMaxMem() != 0) {
            operator.putStatistics(WORKAREA_MAX_MEN, stats.getWorkareaMaxMem() + "");
        }
        if (stats.getWorkareaMaxTempSeg() != 0) {
            operator.putStatistics(WORKAREA_MAX_TEMPSEG, stats.getWorkareaMaxTempSeg() + "");
        }
        if (stats.getStarts() != 0) {
            operator.putStatistics(START_TIMES, stats.getStarts() + "");
        }
        operator.putStatistics(IO_READ_BYTES, stats.getOtherstats().get(IO_READ_BYTES));
        operator.putStatistics(BYTES_IN_TOTAL, stats.getOtherstats().get(BYTES_IN_TOTAL));
        operator.putStatistics(SSSTORE_ROWS_IN_TOTAL, stats.getOtherstats().get(SSSTORE_ROWS_IN_TOTAL));
        operator.putStatistics(MEMSTORE_ROWS_IN_TOTAL, stats.getOtherstats().get(MEMSTORE_ROWS_IN_TOTAL));
        graph.addStatistics(IO_READ_BYTES, stats.getOtherstats().get(IO_READ_BYTES));
        graph.addStatistics(BYTES_IN_TOTAL, stats.getOtherstats().get(BYTES_IN_TOTAL));
        graph.addStatistics(SSSTORE_ROWS_IN_TOTAL, stats.getOtherstats().get(SSSTORE_ROWS_IN_TOTAL));
        graph.addStatistics(MEMSTORE_ROWS_IN_TOTAL, stats.getOtherstats().get(MEMSTORE_ROWS_IN_TOTAL));
        // attributes
        if (MapUtils.isNotEmpty(stats.getOtherstats())) {
            operator.putAttribute(OTHER_STATS, stats.getOtherstats().entrySet()
                    .stream().map(entry -> String.format("%s : %s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
        }
        // output rows
        List<PlanGraphEdge> inEdges = operator.getInEdges();
        if (inEdges != null && inEdges.size() == 1) {
            inEdges.get(0).setWeight(Float.valueOf(stats.getOutputRows()));
        }
    }

}
