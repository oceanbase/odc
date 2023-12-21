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

package com.oceanbase.odc.service.common;

import static com.oceanbase.odc.common.util.StringUtils.getBriefSql;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.DRUID_ACTIVE_COUNT_MORE_THAN_80_PERCENT;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.DRUID_MONITOR_ERROR;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.DRUID_WAIT_THREAD_COUNT_MORE_THAN_0;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.METHOD_TOO_LONG_EXECUTE_TIME;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.METHOD_TOO_MUCH_JDBC_EXECUTE_COUNT;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.SQL_EXECUTE_ERROR;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.SQL_TOO_LONG_EXECUTE_TIME;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.SQL_TOO_LONG_SQL_PARAMETERS;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidStatManagerFacade;
import com.alibaba.druid.support.spring.stat.SpringStatManager;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(value = "odc.system.monitor.enabled", havingValue = "true")
public class DruidMonitor implements InitializingBean {

    private static final Logger SQL_LOGGER = LoggerFactory.getLogger("SqlMonitorLogger");
    private static final Logger METHOD_LOGGER = LoggerFactory.getLogger("MethodMonitorLogger");

    private final DruidStatManagerFacade druidStatManagerFacade = DruidStatManagerFacade.getInstance();

    private final SpringStatManager springStatManager = SpringStatManager.getInstance();

    public static final String CONFIG_PREFIX = "odc.monitor.druid";

    public static final String DATA_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private DruidDataSource druidDataSource;

    @Autowired
    private SystemConfigService systemConfigService;

    private List<Configuration> configurations;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Override
    public void afterPropertiesSet() throws Exception {
        druidStatManagerFacade.setResetEnable(true);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                doMonitor();
            } catch (Exception e) {
                AlarmUtils.alarm(DRUID_MONITOR_ERROR, e);
                log.info("monitor error", e);
            }
        }, 1, 2, TimeUnit.MINUTES);
    }

    public void doMonitor() {
        configurations = systemConfigService.queryByKeyPrefix(CONFIG_PREFIX);
        monitorMethod();
        monitorSqlStatData();
        monitorDruidDataSource();
    }

    @PreDestroy
    public void preDestroy() {
        ExecutorUtils.gracefulShutdown(scheduledExecutorService, "druidMonitor", 5);
    }

    private void monitorSqlStatData() {
        List<Map<String, Object>> sqlStatDataList = druidStatManagerFacade.getSqlStatDataList(null);
        // reset all stats
        druidStatManagerFacade.resetAll();

        List<DruidSQLStats> sqlStatVo = sqlStatDataList.stream().map(DruidSQLStats::mapToVO).collect(
                Collectors.toList());
        if (getBooleanMonitorConfig(DruidMonitorConfig.SQL_LOG_ENABLED)) {
            sqlStatVo.forEach(s -> SQL_LOGGER.info(s.buildLog()));
        }
        monitorTooLongParameter(sqlStatVo);
        monitorAvgExecuteTime(sqlStatVo);
        monitorMaxExecuteTime(sqlStatVo);
        monitorSqlError(sqlStatVo);

    }

    private void monitorTooLongParameter(List<DruidSQLStats> sqlStatVo) {
        List<String> msg = sqlStatVo.stream().filter(v -> v.getLastSlowParameters() != null)
                .filter(t -> t.getLastSlowParameters()
                        .length() > getLongMonitorConfig(DruidMonitorConfig.SQL_PARAMETER_SIZE))
                .map(DruidSQLStats::buildTooLongParameter)
                .collect(Collectors.toList());
        if (!msg.isEmpty()) {
            AlarmUtils.alarm(SQL_TOO_LONG_SQL_PARAMETERS, msg.toString());
        }
    }

    private void monitorAvgExecuteTime(List<DruidSQLStats> sqlStatVo) {
        List<String> avgExecuteTooLong = sqlStatVo.stream().filter(
                v -> v.computeAvgExecuteTime() > getLongMonitorConfig(DruidMonitorConfig.SQL_AVG_EXECUTE_TIME))
                .map(DruidSQLStats::buildTooLongAvgExecuteTimeMsg).collect(Collectors.toList());
        if (!avgExecuteTooLong.isEmpty()) {
            AlarmUtils.alarm(SQL_TOO_LONG_EXECUTE_TIME, "too long avg execute time + " + avgExecuteTooLong);
        }
    }

    private void monitorMaxExecuteTime(List<DruidSQLStats> sqlStatVo) {
        List<String> maxExecuteTooLong = sqlStatVo.stream().filter(
                v -> v.getMaxTimespan() > getLongMonitorConfig(DruidMonitorConfig.SQL_EXECUTE_TIME))
                .map(DruidSQLStats::buildMaxExecuteTimeMsg)
                .collect(Collectors.toList());
        if (!maxExecuteTooLong.isEmpty()) {
            AlarmUtils.alarm(SQL_TOO_LONG_EXECUTE_TIME, "too long max execute time + " + maxExecuteTooLong);
        }
    }

    private void monitorSqlError(List<DruidSQLStats> sqlStatVo) {
        List<String> sqlError = sqlStatVo.stream().filter(
                v -> v.getErrorCount() > 0).map(DruidSQLStats::buildSqlErrorMsg)
                .collect(Collectors.toList());
        if (!sqlError.isEmpty()) {
            AlarmUtils.alarm(SQL_EXECUTE_ERROR, sqlError.toString());
        }
    }

    private Long getLongMonitorConfig(DruidMonitorConfig druidMonitorConfig) {
        return Long.valueOf(getMonitorConfig(druidMonitorConfig));
    }

    private boolean getBooleanMonitorConfig(DruidMonitorConfig druidMonitorConfig) {
        return Boolean.parseBoolean(getMonitorConfig(druidMonitorConfig));
    }

    private String getMonitorConfig(DruidMonitorConfig druidMonitorConfig) {
        return configurations.stream()
                .filter(c -> druidMonitorConfig.key.equals(c.getKey()))
                .map(Configuration::getValue)
                .findFirst().orElse(druidMonitorConfig.defaultValue);
    }

    private void monitorDruidDataSource() {
        if (druidDataSource.getWaitThreadCount() > 0) {
            AlarmUtils.alarm(DRUID_WAIT_THREAD_COUNT_MORE_THAN_0,
                    "DRUID_WAIT_THREAD_COUNT=" + druidDataSource.getWaitThreadCount());
        }
        int activeCount = druidDataSource.getActiveCount();
        int maxActive = druidDataSource.getMaxActive();
        if (maxActive * 0.8 > 0 && activeCount > maxActive * 0.8) {
            AlarmUtils.alarm(DRUID_ACTIVE_COUNT_MORE_THAN_80_PERCENT,
                    "activeCount=" + activeCount + ",maxActive=" + maxActive);
        }
    }

    enum DruidMonitorConfig {
        SQL_AVG_EXECUTE_TIME("odc.monitor.druid.sql.avg-execute-time", "3000"),
        SQL_EXECUTE_TIME("odc.monitor.druid.sql.execute-time", "10000"),
        SQL_PARAMETER_SIZE("odc.monitor.druid.sql.parameter-size", "1000"),
        SQL_LOG_ENABLED("odc.monitor.druid.sql.log.enabled", "true"),
        METHOD_LOG_ENABLED("odc.monitor.druid.method.log.enabled", "true"),
        METHOD_JDBC_COUNT("odc.monitor.druid.method.jdbc-count", "100"),
        METHOD_JDBC_EXECUTE_TIME("odc.monitor.druid.method.jdbc-execute-time", "3000"),
        METHOD_EXECUTE_TIME("odc.monitor.druid.method.execute-time", "5000");

        private final String defaultValue;
        private final String key;

        DruidMonitorConfig(String key, String defaultValue) {
            Preconditions.checkArgument(key.startsWith(CONFIG_PREFIX));
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    static final class DruidSQLStateConstants {
        public static final String EXECUTE_AND_RESULT_SET_HOLD_TIME = "ExecuteAndResultSetHoldTime";
        public static final String EFFECTED_ROW_COUNT_HISTOGRAM = "EffectedRowCountHistogram";
        public static final String LAST_ERROR_MESSAGE = "LastErrorMessage";
        public static final String HISTOGRAM = "Histogram";
        public static final String INPUT_STREAM_OPEN_COUNT = "InputStreamOpenCount";
        public static final String BATCH_SIZE_TOTAL = "BatchSizeTotal";
        public static final String FETCH_ROW_COUNT_MAX = "FetchRowCountMax";
        public static final String ERROR_COUNT = "ErrorCount";
        public static final String BATCH_SIZE_MAX = "BatchSizeMax";
        public static final String LAST_ERROR_TIME = "LastErrorTime";
        public static final String READER_OPEN_COUNT = "ReaderOpenCount";
        public static final String EFFECTED_ROW_COUNT_MAX = "EffectedRowCountMax";
        public static final String LAST_ERROR_CLASS = "LastErrorClass";
        public static final String IN_TRANSACTION_COUNT = "InTransactionCount";
        public static final String LAST_ERROR_STACK_TRACE = "LastErrorStackTrace";
        public static final String RESULT_SET_HOLD_TIME = "ResultSetHoldTime";
        public static final String TOTAL_TIME = "TotalTime";
        public static final String CONCURRENT_MAX = "ConcurrentMax";
        public static final String RUNNING_COUNT = "RunningCount";
        public static final String FETCH_ROW_COUNT = "FetchRowCount";
        public static final String MAX_TIME_SPAN_OCCUR_TIME = "MaxTimespanOccurTime";
        public static final String LAST_SLOW_PARAMETERS = "LastSlowParameters";
        public static final String READ_BYTES_LENGTH = "ReadBytesLength";
        public static final String DB_TYPE = "DbType";
        public static final String DATASOURCE = "DataSource";
        public static final String SQL = "SQL";
        public static final String LAST_ERROR = "LastError";
        public static final String MAX_TIME_SPAN = "MaxTimespan";
        public static final String BLOB_OPEN_COUNT = "BlobOpenCount";
        public static final String EXECUTE_COUNT = "ExecuteCount";
        public static final String EFFECTED_ROW_COUNT = "EffectedRowCount";
        public static final String READ_STRING_LENGTH = "ReadStringLength";
        public static final String EXECUTE_AND_RESULT_HOLD_TIME_HISTOGRAM = "ExecuteAndResultHoldTimeHistogram";
        public static final String FILE = "File";
        public static final String CLOB_OPEN_COUNT = "ClobOpenCount";
        public static final String LAST_TIME = "LastTime";
        public static final String FETCH_ROW_COUNT_HISTOGRAM = "FetchRowCountHistogram";

        private DruidSQLStateConstants() {}
    }

    @Data
    static class DruidSQLStats {
        private Long executeAndResultSetHoldTime;
        private long[] effectedRowCountHistogram;
        private String lastErrorMessage;
        private long[] histogram;
        private Long inputStreamOpenCount;
        private Long batchSizeTotal;
        private Long fetchRowCountMax;
        private Long errorCount;
        private Long batchSizeMax;
        private String url;
        private String name;
        private Object lastErrorTime;
        private Long readerOpenCount;
        private Long effectedRowCountMax;
        private String lastErrorClass;
        private Long inTransactionCount;
        private String lastErrorStackTrace;
        private Long resultSetHoldTime;
        private Long totalTime;
        private Integer id;
        private Long concurrentMax;
        private Long runningCount;
        private Long fetchRowCount;
        private Date maxTimespanOccurTime;
        private String lastSlowParameters;
        private Long readBytesLength;
        private String dbType;
        private String dataSource;
        private String sql;
        private Long hash;
        private Object lastError;
        private Long maxTimespan;
        private Long blobOpenCount;
        private Long executeCount;
        private Long effectedRowCount;
        private Long readStringLength;
        private long[] executeAndResultHoldTimeHistogram;
        private String file;
        private Long clobOpenCount;
        private Date lastTime;
        private long[] fetchRowCountHistogram;

        public Long computeAvgExecuteTime() {
            return executeCount == 0 ? 0 : totalTime / executeCount;
        }

        public String buildLog() {
            return new StringBuilder()
                    .append("executeAndResultSetHoldTime=").append(executeAndResultSetHoldTime).append(", ")
                    .append("effectedRowCountHistogram=").append(Arrays.toString(effectedRowCountHistogram))
                    .append(", ")
                    .append("lastErrorMessage=").append(lastErrorMessage).append(", ")
                    .append("histogram=").append(Arrays.toString(histogram)).append(", ")
                    .append("inputStreamOpenCount=").append(inputStreamOpenCount).append(", ")
                    .append("batchSizeTotal=").append(batchSizeTotal).append(", ")
                    .append("fetchRowCountMax=").append(fetchRowCountMax).append(", ")
                    .append("errorCount=").append(errorCount).append(", ")
                    .append("batchSizeMax=").append(batchSizeMax).append(", ")
                    .append("url=").append(url).append(", ")
                    .append("name=").append(name).append(", ")
                    .append("lastErrorTime=").append(lastErrorTime).append(", ")
                    .append("readerOpenCount=").append(readerOpenCount).append(", ")
                    .append("effectedRowCountMax=").append(effectedRowCountMax).append(", ")
                    .append("lastErrorClass=").append(lastErrorClass).append(", ")
                    .append("inTransactionCount=").append(inTransactionCount).append(", ")
                    .append("lastErrorStackTrace=").append(lastErrorStackTrace).append(", ")
                    .append("resultSetHoldTime=").append(resultSetHoldTime).append(", ")
                    .append("totalTime=").append(totalTime).append(", ")
                    .append("id=").append(id).append(", ")
                    .append("concurrentMax=").append(concurrentMax).append(", ")
                    .append("runningCount=").append(runningCount).append(", ")
                    .append("fetchRowCount=").append(fetchRowCount).append(", ")
                    .append("maxTimespanOccurTime=")
                    .append(maxTimespanOccurTime != null
                            ? new SimpleDateFormat(DATA_FORMAT_PATTERN).format(maxTimespanOccurTime)
                            : "null")
                    .append(", ")
                    .append("lastSlowParameters=").append(lastSlowParameters).append(", ")
                    .append("readBytesLength=").append(readBytesLength).append(", ")
                    .append("dbType=").append(dbType).append(", ")
                    .append("dataSource=").append(dataSource).append(", ")
                    .append("sql=[").append(StringUtils.removeWhitespace(sql)).append("], ")
                    .append("hash=").append(hash).append(", ")
                    .append("lastError=[")
                    .append(StringUtils.removeWhitespace(lastError == null ? "" : lastError.toString())).append("], ")
                    .append("maxTimespan=").append(maxTimespan).append(", ")
                    .append("blobOpenCount=").append(blobOpenCount).append(", ")
                    .append("executeCount=").append(executeCount).append(", ")
                    .append("effectedRowCount=").append(effectedRowCount).append(", ")
                    .append("readStringLength=").append(readStringLength).append(", ")
                    .append("executeAndResultHoldTimeHistogram=")
                    .append(Arrays.toString(executeAndResultHoldTimeHistogram)).append(", ")
                    .append("file=").append(file).append(", ")
                    .append("clobOpenCount=").append(clobOpenCount).append(", ")
                    .append("lastTime=")
                    .append(lastTime != null ? new SimpleDateFormat(DATA_FORMAT_PATTERN).format(lastTime) : "null")
                    .append(", ")
                    .append("fetchRowCountHistogram=").append(Arrays.toString(fetchRowCountHistogram))
                    .toString();
        }

        private String getSqlIdentification() {
            return "file=" + file + ",sql=" + getBriefSql(sql, 100);
        }

        public String buildTooLongParameter() {
            return getSqlIdentification();
        }

        public String buildTooLongAvgExecuteTimeMsg() {
            return getSqlIdentification() + ",avgExecuteTime=" + computeAvgExecuteTime() + ";";
        }

        public String buildMaxExecuteTimeMsg() {
            return getSqlIdentification() + ",maxExecuteTime=" + maxTimespan
                    + "，maxTimespanOccurTime="
                    + maxTimespanOccurTime
                    + ";";
        }

        public String buildSqlErrorMsg() {
            return getSqlIdentification() + ",errorCount=" + errorCount
                    + "，lastErrorMessage="
                    + lastErrorMessage
                    + ";";
        }

        public static DruidSQLStats mapToVO(Map<String, Object> map) {
            DruidSQLStats vo = new DruidSQLStats();
            vo.setExecuteAndResultSetHoldTime(
                    default0((Long) map.get(DruidSQLStateConstants.EXECUTE_AND_RESULT_SET_HOLD_TIME)));
            vo.setEffectedRowCountHistogram(
                    defaultEmptyArray((long[]) map.get(DruidSQLStateConstants.EFFECTED_ROW_COUNT_HISTOGRAM)));
            vo.setLastErrorMessage((String) map.get(DruidSQLStateConstants.LAST_ERROR_MESSAGE));
            vo.setHistogram(defaultEmptyArray((long[]) map.get(DruidSQLStateConstants.HISTOGRAM)));
            vo.setInputStreamOpenCount(default0((Long) map.get(DruidSQLStateConstants.INPUT_STREAM_OPEN_COUNT)));
            vo.setBatchSizeTotal(default0((Long) map.get(DruidSQLStateConstants.BATCH_SIZE_TOTAL)));
            vo.setFetchRowCountMax(default0((Long) map.get(DruidSQLStateConstants.FETCH_ROW_COUNT_MAX)));
            vo.setErrorCount(default0((Long) map.get(DruidSQLStateConstants.ERROR_COUNT)));
            vo.setBatchSizeMax(default0((Long) map.get(DruidSQLStateConstants.BATCH_SIZE_MAX)));
            vo.setLastErrorTime(map.get(DruidSQLStateConstants.LAST_ERROR_TIME));
            vo.setReaderOpenCount(default0((Long) map.get(DruidSQLStateConstants.READER_OPEN_COUNT)));
            vo.setEffectedRowCountMax(default0((Long) map.get(DruidSQLStateConstants.EFFECTED_ROW_COUNT_MAX)));
            vo.setLastErrorClass((String) map.get(DruidSQLStateConstants.LAST_ERROR_CLASS));
            vo.setInTransactionCount(default0((Long) map.get(DruidSQLStateConstants.IN_TRANSACTION_COUNT)));
            vo.setLastErrorStackTrace((String) map.get(DruidSQLStateConstants.LAST_ERROR_STACK_TRACE));
            vo.setResultSetHoldTime(default0((Long) map.get(DruidSQLStateConstants.RESULT_SET_HOLD_TIME)));
            vo.setTotalTime(default0((Long) map.get(DruidSQLStateConstants.TOTAL_TIME)));
            vo.setConcurrentMax(default0((Long) map.get(DruidSQLStateConstants.CONCURRENT_MAX)));
            vo.setRunningCount(default0((Long) map.get(DruidSQLStateConstants.RUNNING_COUNT)));
            vo.setFetchRowCount(default0((Long) map.get(DruidSQLStateConstants.FETCH_ROW_COUNT)));
            vo.setMaxTimespanOccurTime((Date) map.get(DruidSQLStateConstants.MAX_TIME_SPAN_OCCUR_TIME));
            vo.setLastSlowParameters((String) map.get(DruidSQLStateConstants.LAST_SLOW_PARAMETERS));
            vo.setReadBytesLength(default0((Long) map.get(DruidSQLStateConstants.READ_BYTES_LENGTH)));
            vo.setDbType((String) map.get(DruidSQLStateConstants.DB_TYPE));
            vo.setDataSource((String) map.get(DruidSQLStateConstants.DATASOURCE));
            vo.setSql((String) map.get(DruidSQLStateConstants.SQL));
            vo.setLastError(map.get(DruidSQLStateConstants.LAST_ERROR));
            vo.setMaxTimespan(default0((Long) map.get(DruidSQLStateConstants.MAX_TIME_SPAN)));
            vo.setBlobOpenCount(default0((Long) map.get(DruidSQLStateConstants.BLOB_OPEN_COUNT)));
            vo.setExecuteCount(default0((Long) map.get(DruidSQLStateConstants.EXECUTE_COUNT)));
            vo.setEffectedRowCount(default0((Long) map.get(DruidSQLStateConstants.EFFECTED_ROW_COUNT)));
            vo.setReadStringLength(default0((Long) map.get(DruidSQLStateConstants.READ_STRING_LENGTH)));
            vo.setExecuteAndResultHoldTimeHistogram(
                    defaultEmptyArray((long[]) map.get(DruidSQLStateConstants.EXECUTE_AND_RESULT_HOLD_TIME_HISTOGRAM)));
            vo.setFile((String) map.get(DruidSQLStateConstants.FILE));
            vo.setClobOpenCount(default0((Long) map.get(DruidSQLStateConstants.CLOB_OPEN_COUNT)));
            vo.setLastTime((Date) map.get(DruidSQLStateConstants.LAST_TIME));
            vo.setFetchRowCountHistogram(
                    defaultEmptyArray((long[]) map.get(DruidSQLStateConstants.FETCH_ROW_COUNT_HISTOGRAM)));
            return vo;
        }
    }

    private void monitorMethod() {
        List<Map<String, Object>> methodStatData = springStatManager.getMethodStatData();
        springStatManager.resetStat();

        List<DruidMethodStats> methodStatVOS =
                methodStatData.stream().map(DruidMethodStats::mapToVO).collect(
                        Collectors.toList());

        if (getBooleanMonitorConfig(DruidMonitorConfig.METHOD_LOG_ENABLED)) {
            methodStatVOS.forEach(v -> METHOD_LOGGER.info(v.buildLog()));
        }

        monitorMethodAvgExecuteTooLong(methodStatVOS);
        monitorMethodJdbcExecuteTooLong(methodStatVOS);
        monitorMethodTooMuchJdbcExecute(methodStatVOS);

    }

    private void monitorMethodAvgExecuteTooLong(List<DruidMethodStats> methodStatVOS) {
        List<String> msg = methodStatVOS.stream()
                .filter(t -> t.computeAvgExecuteTime() > getLongMonitorConfig(DruidMonitorConfig.METHOD_EXECUTE_TIME))
                .map(
                        DruidMethodStats::buildTooLongAvgExecuteTimeMsg)
                .collect(
                        Collectors.toList());
        if (!msg.isEmpty()) {
            AlarmUtils.alarm(METHOD_TOO_LONG_EXECUTE_TIME, "method too long avg execute time" + msg);
        }
    }

    private void monitorMethodJdbcExecuteTooLong(List<DruidMethodStats> methodStatVOS) {
        List<String> msg = methodStatVOS.stream().filter(
                t -> t.computeAvgJdbcExecuteTime() > getLongMonitorConfig(DruidMonitorConfig.METHOD_JDBC_EXECUTE_TIME))
                .map(
                        DruidMethodStats::buildTooLongJdbcAvgExecuteTimeMsg)
                .collect(
                        Collectors.toList());
        if (!msg.isEmpty()) {
            AlarmUtils.alarm(METHOD_TOO_LONG_EXECUTE_TIME, "method too long avg jdbc execute time" + msg);
        }
    }

    private void monitorMethodTooMuchJdbcExecute(List<DruidMethodStats> methodStatVOS) {
        List<String> msg = methodStatVOS.stream()
                .filter(t -> t
                        .computeAvgJdbcExecuteCount() > getLongMonitorConfig(DruidMonitorConfig.METHOD_JDBC_COUNT))
                .map(
                        DruidMethodStats::buildTooMuchJdbcExecuteCountMsg)
                .collect(
                        Collectors.toList());
        if (!msg.isEmpty()) {
            AlarmUtils.alarm(METHOD_TOO_MUCH_JDBC_EXECUTE_COUNT, "method too much jdbc execute count" + msg);
        }
    }


    public static Long default0(@Nullable Long value) {
        return MoreObjects.firstNonNull(value, 0L);
    }

    public static Integer default0(@Nullable Integer value) {
        return MoreObjects.firstNonNull(value, 0);
    }

    public static long[] defaultEmptyArray(@Nullable long[] value) {
        return MoreObjects.firstNonNull(value, new long[] {});
    }

    @Data
    static final class DruidMethodStatConstants {
        public static final String CLASS = "Class";
        public static final String METHOD = "Method";
        public static final String RUNNING_COUNT = "RunningCount";
        public static final String CONCURRENT_MAX = "ConcurrentMax";
        public static final String EXECUTE_COUNT = "ExecuteCount";
        public static final String EXECUTE_ERROR_COUNT = "ExecuteErrorCount";
        public static final String EXECUTE_TIME_MILLIS = "ExecuteTimeMillis";
        public static final String JDBC_COMMIT_COUNT = "JdbcCommitCount";
        public static final String JDBC_READ_ONLY_COUNT = "JdbcReadOnlyCount";
        public static final String JDBC_ROLLBACK_COUNT = "JdbcRollbackCount";
        public static final String JDBC_POOL_CONNECTION_OPEN_COUNT = "JdbcPoolConnectionOpenCount";
        public static final String JDBC_POOL_CONNECTION_CLOSE_COUNT = "JdbcPoolConnectionCloseCount";
        public static final String JDBC_RESULT_SET_OPEN_COUNT = "JdbcResultSetOpenCount";
        public static final String JDBC_RESULT_SET_CLOSE_COUNT = "JdbcResultSetCloseCount";
        public static final String JDBC_EXECUTE_COUNT = "JdbcExecuteCount";
        public static final String JDBC_EXECUTE_ERROR_COUNT = "JdbcExecuteErrorCount";
        public static final String JDBC_EXECUTE_TIME_MILLIS = "JdbcExecuteTimeMillis";
        public static final String JDBC_FETCH_ROW_COUNT = "JdbcFetchRowCount";
        public static final String JDBC_UPDATE_COUNT = "JdbcUpdateCount";
        public static final String LAST_ERROR = "LastError";
        public static final String LAST_ERROR_TIME = "LastErrorTime";
        public static final String HISTOGRAM = "Histogram";

        private DruidMethodStatConstants() {}
    }

    @Data
    public static class DruidMethodStats {
        private String clazz;
        private String method;
        private Integer runningCount;
        private Integer concurrentMax;
        private Long executeCount;
        private Long executeErrorCount;
        private Long executeTimeMillis;
        private Long jdbcCommitCount;
        private Long jdbcReadOnlyCount;
        private Long jdbcRollbackCount;
        private Long jdbcPoolConnectionOpenCount;
        private Long jdbcPoolConnectionCloseCount;
        private Long jdbcResultSetOpenCount;
        private Long jdbcResultSetCloseCount;
        private Long jdbcExecuteCount;
        private Long jdbcExecuteErrorCount;
        private Long jdbcExecuteTimeMillis;
        private Long jdbcFetchRowCount;
        private Long jdbcUpdateCount;
        private Object lastError;
        private Date lastErrorTime;
        private long[] histogram;

        public String buildLog() {
            return new StringBuilder()
                    .append("clazz=").append(clazz).append(", ")
                    .append("method=").append(method).append(", ")
                    .append("runningCount=").append(runningCount).append(", ")
                    .append("concurrentMax=").append(concurrentMax).append(", ")
                    .append("executeCount=").append(executeCount).append(", ")
                    .append("executeErrorCount=").append(executeErrorCount).append(", ")
                    .append("executeTimeMillis=").append(executeTimeMillis).append(", ")
                    .append("jdbcCommitCount=").append(jdbcCommitCount).append(", ")
                    .append("jdbcReadOnlyCount=").append(jdbcReadOnlyCount).append(", ")
                    .append("jdbcRollbackCount=").append(jdbcRollbackCount).append(", ")
                    .append("jdbcPoolConnectionOpenCount=").append(jdbcPoolConnectionOpenCount).append(", ")
                    .append("jdbcPoolConnectionCloseCount=").append(jdbcPoolConnectionCloseCount).append(", ")
                    .append("jdbcResultSetOpenCount=").append(jdbcResultSetOpenCount).append(", ")
                    .append("jdbcResultSetCloseCount=").append(jdbcResultSetCloseCount).append(", ")
                    .append("jdbcExecuteCount=").append(jdbcExecuteCount).append(", ")
                    .append("jdbcExecuteErrorCount=").append(jdbcExecuteErrorCount).append(", ")
                    .append("jdbcExecuteTimeMillis=").append(jdbcExecuteTimeMillis).append(", ")
                    .append("jdbcFetchRowCount=").append(jdbcFetchRowCount).append(", ")
                    .append("jdbcUpdateCount=").append(jdbcUpdateCount).append(", ")
                    .append("lastError=[")
                    .append(lastError == null ? "" : StringUtils.removeWhitespace(lastError.toString())).append("], ")
                    .append("lastErrorTime=")
                    .append(lastErrorTime != null ? new SimpleDateFormat(DATA_FORMAT_PATTERN).format(lastErrorTime)
                            : "null")
                    .append(", ")
                    .append("histogram=").append(histogram != null ? Arrays.toString(histogram) : "null")
                    .toString();

        }

        public Long computeAvgExecuteTime() {
            return executeCount > 0 ? executeTimeMillis / executeCount : 0;
        }

        public Long computeAvgJdbcExecuteTime() {
            return executeCount > 0 ? jdbcExecuteTimeMillis / executeCount : 0;
        }

        public Long computeAvgJdbcExecuteCount() {
            return executeCount > 0 ? jdbcExecuteCount / executeCount : 0;
        }

        private String getMethodIdentification() {
            return "class=" + clazz + "，method=" + method;
        }

        public String buildTooLongAvgExecuteTimeMsg() {
            return getMethodIdentification() + ", avgExecuteTime=" + computeAvgExecuteTime() + ";";
        }

        public String buildTooLongJdbcAvgExecuteTimeMsg() {
            return getMethodIdentification() + ", avgJdbcExecuteTime=" + computeAvgJdbcExecuteTime() + ";";
        }

        public String buildTooMuchJdbcExecuteCountMsg() {
            return getMethodIdentification() + ", avgJdbcExecuteCount=" + computeAvgJdbcExecuteCount()
                    + ";";
        }

        public static DruidMethodStats mapToVO(Map<String, Object> map) {
            DruidMethodStats vo = new DruidMethodStats();
            vo.setClazz((String) map.get(DruidMethodStatConstants.CLASS));
            vo.setMethod((String) map.get(DruidMethodStatConstants.METHOD));
            vo.setRunningCount(default0((Integer) map.get(DruidMethodStatConstants.RUNNING_COUNT)));
            vo.setConcurrentMax(default0((Integer) map.get(DruidMethodStatConstants.CONCURRENT_MAX)));
            vo.setExecuteCount(default0((Long) map.get(DruidMethodStatConstants.EXECUTE_COUNT)));
            vo.setExecuteErrorCount(default0((Long) map.get(DruidMethodStatConstants.EXECUTE_ERROR_COUNT)));
            vo.setExecuteTimeMillis(default0((Long) map.get(DruidMethodStatConstants.EXECUTE_TIME_MILLIS)));
            vo.setJdbcCommitCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_COMMIT_COUNT)));
            vo.setJdbcReadOnlyCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_READ_ONLY_COUNT)));
            vo.setJdbcRollbackCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_ROLLBACK_COUNT)));
            vo.setJdbcPoolConnectionOpenCount(
                    default0((Long) map.get(DruidMethodStatConstants.JDBC_POOL_CONNECTION_OPEN_COUNT)));
            vo.setJdbcPoolConnectionCloseCount(
                    default0((Long) map.get(DruidMethodStatConstants.JDBC_POOL_CONNECTION_CLOSE_COUNT)));
            vo.setJdbcResultSetOpenCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_RESULT_SET_OPEN_COUNT)));
            vo.setJdbcResultSetCloseCount(
                    default0((Long) map.get(DruidMethodStatConstants.JDBC_RESULT_SET_CLOSE_COUNT)));
            vo.setJdbcExecuteCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_EXECUTE_COUNT)));
            vo.setJdbcExecuteErrorCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_EXECUTE_ERROR_COUNT)));
            vo.setJdbcExecuteTimeMillis(default0((Long) map.get(DruidMethodStatConstants.JDBC_EXECUTE_TIME_MILLIS)));
            vo.setJdbcFetchRowCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_FETCH_ROW_COUNT)));
            vo.setJdbcUpdateCount(default0((Long) map.get(DruidMethodStatConstants.JDBC_UPDATE_COUNT)));
            vo.setLastError(map.get(DruidMethodStatConstants.LAST_ERROR));
            vo.setLastErrorTime((Date) map.get(DruidMethodStatConstants.LAST_ERROR_TIME));
            vo.setHistogram(defaultEmptyArray((long[]) map.get(DruidMethodStatConstants.HISTOGRAM)));
            return vo;
        }
    }



}
