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
package com.oceanbase.odc.service.resultset;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.DataMaskingUtil;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.util.SqlRewriteUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/11/22 下午3:01
 * @Description: [ResultSetExportTaskManager implements by OBDumper]
 */
@Slf4j
@Component
public class DumperResultSetExportTaskManager implements ResultSetExportTaskManager {
    private final static Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private final static String DEFAULT_FILE_PREFIX = "CUSTOM_SQL";

    private ExecutorService executor;
    private File logDir;

    @Value("${odc.resultset.export.thread-count:10}")
    private int threadCount;

    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    @Autowired
    private DataTransferProperties dataTransferProperties;

    @Autowired
    private DataMaskingService maskingService;

    @PostConstruct
    public void init() {
        log.info("DumperResultSetExportTaskManager start initializing...");
        executor = Executors.newFixedThreadPool(threadCount);
        log.info("DumperResultSetExportTaskManager initialized successfully");
    }

    @PreDestroy
    public void destroy() {
        log.info("DumperResultSetExportTaskManager start destroying...");
        executor.shutdown();
        log.info("DumperResultSetExportTaskManager destroyed successfully");
    }

    @Override
    public ResultSetExportTaskContext start(ConnectionConfig connectionConfig, ResultSetExportTaskParameter parameter,
            String taskId) {
        initLogPath(taskId);

        TraceContextHolder.put(DataTransferConstants.LOG_PATH_NAME, logDir.getPath());
        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        ConnectionSessionUtil.setCurrentSchema(session, parameter.getDatabase());
        try {
            File workingDir = new File(FileManager.generateDirPath(FileBucket.RESULT_SET, taskId));
            if (!workingDir.exists()) {
                workingDir.mkdir();
            }
            if (parameter.getFileFormat() != DataTransferFormat.SQL || parameter.getTableName() == null) {
                parameter.setTableName(DEFAULT_FILE_PREFIX);
            }
            String fileName =
                    StringUtils.isBlank(parameter.getFileName()) ? "result_set" : parameter.getFileName().trim();
            String extension = parameter.getFileFormat().getExtension();
            String fileNameWithExtension = fileName.endsWith(extension) ? fileName : fileName + extension;
            parameter.setFileName(fileNameWithExtension);
            /*
             * set mask config if necessary
             */
            if (maskingService.isMaskingEnabled()) {
                parameter.setRowDataMaskingAlgorithms(getRowDataMaskingAlgorithms(parameter.getSql(), session));
            }
            parameter.setSql(SqlRewriteUtil.addQueryLimit(parameter.getSql(), session, parameter.getMaxRows()));

            ResultSetExportTask task = new ResultSetExportTask(workingDir, logDir, parameter, session,
                    cloudObjectStorageService, dataTransferProperties);
            return new ResultSetExportTaskContext(executor.submit(task), task);

        } catch (Exception e) {
            LOGGER.warn("Error occurred when start task.", e);
            throw e;
        } finally {
            session.expire();
            TraceContextHolder.clear();
        }
    }

    private void initLogPath(String taskId) {
        if (logDir == null) {
            logDir = new File(MoreObjects.firstNonNull(SystemUtils.getEnvOrProperty("odc.log.directory"), "./log")
                    + "/result-set-export/" + taskId);
            if (logDir.exists()) {
                FileUtils.deleteQuietly(logDir);
            }
        }
    }

    private List<MaskingAlgorithm> getRowDataMaskingAlgorithms(String sql, ConnectionSession session) {
        try {
            List<Set<SensitiveColumn>> sensitiveColumns = maskingService.getResultSetSensitiveColumns(sql, session);
            if (DataMaskingUtil.isSensitiveColumnExists(sensitiveColumns)) {
                return maskingService.getResultSetMaskingAlgorithms(sensitiveColumns);
            }
        } catch (Exception e) {
            // Eat exception and skip data masking
            log.warn("Failed to set masking algorithm, sql={}", sql, e);
        }
        return Collections.emptyList();
    }

}
