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
package com.oceanbase.odc.plugin.connect.obmysql.diagnose;

import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.BYTES_IN_TOTAL;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.CHANGE_TIME;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.CPU_TIME;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.DB_TIME;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.IO_READ_BYTES;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.IO_WAIT_TIME;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.MEMSTORE_ROWS_IN_TOTAL;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.OTHER_STATS;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.OUTPUT_ROWS;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.PARALLEL;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.PROCESS_NAME;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.SKEWNESS;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.SSSTORE_ROWS_IN_TOTAL;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.START_TIMES;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.WORKAREA_MAX_MEN;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.WORKAREA_MAX_TEMPSEG;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Comparators;
import com.oceanbase.odc.common.util.TimespanFormatUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.model.QueryStatus;
import com.oceanbase.odc.core.shared.model.SqlPlanMonitor;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraph;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraphEdge;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraphOperator;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/6/4
 */
@Slf4j
public class QueryProfileHelper {

    public static void refreshGraph(PlanGraph graph, List<SqlPlanMonitor> spmStats) {
        String traceId = graph.getTraceId();
        try {
            replaceSPMStatsIntoProfile(spmStats, graph);
        } catch (Exception e) {
            log.warn("Failed to get runtime statistics with OB trace_id={}.", traceId, e);
            return;
        }

        Map<String, List<String>> topNodes = new HashMap<>();
        graph.setTopNodes(topNodes);
        topNodes.put("duration", graph.getVertexes()
                .stream().filter(o -> o.getDuration() != null)
                .collect(Comparators.greatest(5, Comparator.comparingLong(PlanGraphOperator::getDuration)))
                .stream().map(PlanGraphOperator::getGraphId).collect(Collectors.toList()));
    }

    private static void replaceSPMStatsIntoProfile(List<SqlPlanMonitor> records, PlanGraph graph) {
        boolean needFilter = false;
        boolean rootOperatorExists = false;
        Timestamp startTs = new Timestamp(0);
        // some inner sql of ob would share trace id with user's query, so we filter out these dirty records
        // by first_refresh_time
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
                        spms.get(0).getPlanLineId(), e);
            }
        }

        SqlPlanMonitor rootOperator = planLineId2Spm.get("0").get(0);
        if (rootOperator.getLastRefreshTime() == null) {
            graph.setStatus(QueryStatus.RUNNING);
            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getCurrentTime().toInstant());
            graph.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(totalTs));
        } else {
            graph.setStatus(QueryStatus.FINISHED);
            Duration totalTs = Duration.between(rootOperator.getFirstRefreshTime().toInstant(),
                    rootOperator.getLastRefreshTime().toInstant());
            graph.putOverview(DB_TIME, TimespanFormatUtil.formatTimespan(totalTs));
        }
    }

    private static void mergeSPMRecords(List<SqlPlanMonitor> spms, PlanGraphOperator operator, PlanGraph graph) {
        List<PlanGraphOperator> subOperators = new ArrayList<>();
        long totalCPUTs = 0;
        long totalIOWaitTs = 0;
        long starts = 0;
        long outputs = 0;
        long maxMem = 0;
        long maxDisk = 0;
        long ioBytes = 0;
        long ssstoreBytes = 0;
        long ssstoreRows = 0;
        long memstoreRows = 0;
        long maxChangeTs = -1;
        long minChangeTs = Integer.MAX_VALUE;
        boolean allNodeFinished = true;
        boolean allNodePreparing = true;

        for (SqlPlanMonitor spm : spms) {
            PlanGraphOperator child = new PlanGraphOperator();
            Long dbTime = spm.getDbTime();
            Long userIOWaitTime = spm.getUserIOWaitTime();
            Long cpuTime = dbTime - userIOWaitTime;
            totalCPUTs += cpuTime;
            totalIOWaitTs += userIOWaitTime;
            child.setDuration(cpuTime);
            child.putOverview(CPU_TIME, cpuTime);
            child.putOverview(IO_WAIT_TIME, userIOWaitTime);
            child.setTitle(String.format("%s(%s)", spm.getSvrIp(), spm.getProcessName()));
            child.setName(operator.getName());
            child.putOverview(PROCESS_NAME, spm.getProcessName());

            if (spm.getLastChangeTime() != null) {
                child.setStatus(QueryStatus.FINISHED);
                if (spm.getFirstChangeTime() != null) {
                    Duration changeTime = Duration.between(spm.getFirstChangeTime().toInstant(),
                            spm.getLastChangeTime().toInstant());
                    child.putOverview(CHANGE_TIME, TimespanFormatUtil.formatTimespan(changeTime));
                    long changeTs = changeTime.toNanos() / 1000;
                    if (changeTs != 0) {
                        maxChangeTs = Math.max(changeTs, maxChangeTs);
                        minChangeTs = Math.min(changeTs, minChangeTs);
                    }
                }
            } else if (spm.getLastRefreshTime() != null) {
                child.setStatus(QueryStatus.FINISHED);
            } else {
                allNodeFinished = false;
                // If the operator is a calculating type like `SCALAR GROUP BY`, when it's running, the
                // FIRST_CHANGE_TIME and LAST_CHANGE_TIME may be null. Then we can determine its status by db_time.
                // And since there maybe some deviation, we set 100 us as the threshold for judging the status.
                if (spm.getFirstChangeTime() == null && spm.getDbTime() < 100) {
                    child.setStatus(QueryStatus.PREPARING);
                } else {
                    child.setStatus(QueryStatus.RUNNING);
                    allNodePreparing = false;
                }
            }

            if (allNodeFinished) {
                operator.setStatus(QueryStatus.FINISHED);
            } else {
                if (allNodePreparing) {
                    operator.setStatus(QueryStatus.PREPARING);
                } else {
                    operator.setStatus(QueryStatus.RUNNING);
                }
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

        operator.setDuration(totalCPUTs);
        operator.putOverview(CPU_TIME, totalCPUTs);
        operator.putOverview(IO_WAIT_TIME, totalIOWaitTs);
        long totalCpuTime = (long) graph.getOverview().getOrDefault(CPU_TIME, 0L) + totalCPUTs;
        graph.putOverview(CPU_TIME, totalCpuTime);
        graph.setDuration(totalCpuTime);
        long totalIOWaitTime = (long) graph.getOverview().getOrDefault(IO_WAIT_TIME, 0L) + totalIOWaitTs;
        graph.putOverview(IO_WAIT_TIME, totalIOWaitTime);

        operator.putOverview(PARALLEL, spms.size());
        if (maxChangeTs > 0) {
            operator.putOverview(SKEWNESS, String.format("%.2f", (maxChangeTs - minChangeTs) * 1f / maxChangeTs));
        }

        // output rows
        List<PlanGraphEdge> inEdges = operator.getInEdges();
        if (inEdges != null && inEdges.size() == 1) {
            inEdges.get(0).setWeight((float) outputs);
        }

        subOperators.sort(Comparator.comparingLong(PlanGraphOperator::getDuration).reversed());
        Map<String, PlanGraphOperator> map = new LinkedHashMap<>();
        for (PlanGraphOperator o : subOperators) {
            map.put(o.getTitle(), o);
        }
        operator.setSubNodes(map);
    }

    private static void setRuntimeStatistics(SqlPlanMonitor stats, PlanGraphOperator operator, PlanGraph graph) {
        // overview
        Long dbTime = stats.getDbTime();
        Long userIOWaitTime = stats.getUserIOWaitTime();
        Long cpuTime = dbTime - userIOWaitTime;
        operator.setDuration(cpuTime);
        operator.putOverview(CPU_TIME, cpuTime);
        long totalCpuTime = (long) graph.getOverview().getOrDefault(CPU_TIME, 0L) + cpuTime;
        graph.putOverview(CPU_TIME, totalCpuTime);
        graph.setDuration(totalCpuTime);
        operator.putOverview(IO_WAIT_TIME, userIOWaitTime);
        long totalIOWaitTime = (long) graph.getOverview().getOrDefault(IO_WAIT_TIME, 0L) + userIOWaitTime;
        graph.putOverview(IO_WAIT_TIME, totalIOWaitTime);
        if (stats.getLastChangeTime() != null) {
            operator.setStatus(QueryStatus.FINISHED);
            if (stats.getFirstChangeTime() != null) {
                Duration changeTime = Duration.between(stats.getFirstChangeTime().toInstant(),
                        stats.getLastChangeTime().toInstant());
                operator.putOverview(CHANGE_TIME, TimespanFormatUtil.formatTimespan(changeTime));
            }
        } else if (stats.getLastRefreshTime() != null) {
            operator.setStatus(QueryStatus.FINISHED);
        } else {
            if (stats.getFirstChangeTime() == null && stats.getDbTime() < 100) {
                operator.setStatus(QueryStatus.PREPARING);
            } else {
                operator.setStatus(QueryStatus.RUNNING);
            }
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
