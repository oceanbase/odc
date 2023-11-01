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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
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
    private final static String DEFAULT_FILE_PREFIX = "CUSTOM_SQL";

    private ExecutorService executor;

    @Value("${odc.resultset.export.thread-count:10}")
    private int threadCount;

    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

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
    public ResultSetExportTaskContext start(ConnectionSession session, ResultSetExportTaskParameter parameter,
            String userId, String taskId) {
        if (parameter.getTableName() == null) {
            parameter.setTableName(DEFAULT_FILE_PREFIX);
        }
        String fileName = StringUtils.isBlank(parameter.getFileName()) ? "result_set" : parameter.getFileName().trim();
        String fileNameWithExtension = fileName + parameter.getFileFormat().getExtension();
        parameter.setFileName(fileNameWithExtension);
        parameter.setSql(SqlRewriteUtil.addQueryLimit(parameter.getSql(), session, parameter.getMaxRows()));

        ResultSetExportTask task = new ResultSetExportTask(taskId, parameter, session, cloudObjectStorageService);
        return new ResultSetExportTaskContext(executor.submit(task), task);
    }

}
