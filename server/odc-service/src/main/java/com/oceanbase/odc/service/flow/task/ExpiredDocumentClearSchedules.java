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
package com.oceanbase.odc.service.flow.task;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.oceanbase.odc.service.connection.ssl.SslExpiredDocumentProvider;
import com.oceanbase.odc.service.datatransfer.file.LocalFileManager;
import com.oceanbase.odc.service.flow.provider.BaseExpiredDocumentProvider;
import com.oceanbase.odc.service.flow.provider.DataTransferExpiredDocumentProvider;
import com.oceanbase.odc.service.flow.provider.DatabaseChangeExpiredFileProvider;
import com.oceanbase.odc.service.flow.provider.MockDataExpiredDocumentProvider;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.MockProperties;
import com.oceanbase.odc.service.resultset.ResultSetExportExpiredFileProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link ExpiredDocumentClearSchedules}
 *
 * @author yh263208
 * @date 2022-03-31 22:28
 * @since ODC_release_3.3.0
 */
@Slf4j
public class ExpiredDocumentClearSchedules {
    /**
     * 轮询时间，默认为1小时
     */
    private static final long DEFAULT_SCHEDULE_CHECK_INTERVAL_MS = 60 * 60 * 1000L;

    private static final int SSL_FILE_EXPIRED_MINUTES = 30;
    private final List<BaseExpiredDocumentProvider> documentProviders = new LinkedList<>();
    @Autowired
    private MockProperties mockProperties;
    @Autowired
    private FlowTaskProperties flowTaskProperties;
    @Autowired
    private LocalFileManager localFileManager;

    @PostConstruct
    public void setup() {
        int fileExpireHours = flowTaskProperties.getFileExpireHours();
        documentProviders.add(new DataTransferExpiredDocumentProvider(localFileManager, fileExpireHours));
        documentProviders.add(new MockDataExpiredDocumentProvider(fileExpireHours, mockProperties));
        documentProviders.add(new DatabaseChangeExpiredFileProvider(fileExpireHours));
        documentProviders.add(new ResultSetExportExpiredFileProvider(fileExpireHours));
        documentProviders.add(new SslExpiredDocumentProvider(SSL_FILE_EXPIRED_MINUTES, TimeUnit.MINUTES));
    }

    @Scheduled(initialDelay = DEFAULT_SCHEDULE_CHECK_INTERVAL_MS, fixedRate = DEFAULT_SCHEDULE_CHECK_INTERVAL_MS)
    public void clearExpiredFiles() {
        documentProviders.forEach(provider -> provider.provide().forEach(file -> {
            log.info("File is expired, will be deleted, file={}, lastModified={}", file.getAbsolutePath(),
                    new Date(file.lastModified()));
            try {
                FileUtils.forceDelete(file);
            } catch (Exception e) {
                log.warn("Failed to delete, file={}", file, e);
            }
        }));
    }

}
