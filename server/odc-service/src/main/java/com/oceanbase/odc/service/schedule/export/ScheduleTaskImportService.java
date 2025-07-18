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
package com.oceanbase.odc.service.schedule.export;

import static com.oceanbase.odc.service.schedule.export.ScheduleTaskImportCallable.LOG_PATH_PATTERN;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.common.FutureCache;
import com.oceanbase.odc.service.exporter.exception.ExtractFileException;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.ImportTaskResult;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskImportRequest;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScheduleTaskImportService {

    @Autowired
    private FutureCache futureCache;

    @Autowired
    private ThreadPoolTaskExecutor commonAsyncTaskExecutor;

    @Autowired
    private ScheduleTaskImporter scheduleTaskImporter;

    @Autowired
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Value("${odc.log.directory:./log}")
    private String logPath;

    @SkipAuthorize("internal usage")
    public String startPreviewImportTask(ScheduleTaskImportRequest request) {
        String previewId = statefulUuidStateIdGenerator.generateCurrentUserIdStateId("scheduleImportReview");
        User user = authenticationFacade.currentUser();
        Future<List<ImportScheduleTaskView>> future = commonAsyncTaskExecutor.submit(
                () -> {
                    try {
                        SecurityContextUtils.setCurrentUser(user);
                        return scheduleTaskImporter.preview(request);
                    } catch (Exception e) {
                        log.info("Preview Import task failed", e);
                        throw e;
                    }

                });
        futureCache.put(previewId, future);
        return previewId;
    }

    @SkipAuthorize("internal usage")
    public List<ImportScheduleTaskView> getPreviewTaskResults(String previewId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(previewId);
        Future<?> future = futureCache.get(previewId);
        if (future == null) {
            return null;
        }
        if (!future.isDone()) {
            return Collections.emptyList();
        }
        try {
            futureCache.invalid(previewId);
            return (List<ImportScheduleTaskView>) future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ExtractFileException) {
                throw (ExtractFileException) e.getCause();
            }
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SkipAuthorize("internal usage")
    public String startImportTask(ScheduleTaskImportRequest request) {
        String previewId = statefulUuidStateIdGenerator.generateCurrentUserIdStateId("scheduleImport");
        User user = authenticationFacade.currentUser();

        Future<List<ImportTaskResult>> future = commonAsyncTaskExecutor.submit(
                new ScheduleTaskImportCallable(user, previewId, scheduleTaskImporter, request));
        futureCache.put(previewId, future);
        return previewId;
    }

    @SkipAuthorize("internal usage")
    public List<ImportTaskResult> getImportTaskResults(String importId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(importId);
        Future<?> future = futureCache.get(importId);
        if (future == null) {
            return null;
        }
        if (!future.isDone()) {
            return Collections.emptyList();
        }
        try {
            futureCache.invalid(importId);
            return (List<ImportTaskResult>) future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ExtractFileException) {
                throw (ExtractFileException) e.getCause();
            }
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SkipAuthorize("internal usage")
    public String getImportLog(String importId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(importId);
        String filePath = String.format(LOG_PATH_PATTERN, logPath, ScheduleTaskImportCallable.WORK_SPACE, importId,
                ScheduleTaskImportCallable.LOG_NAME);
        File logFile = new File(filePath);
        return LogUtils.getLatestLogContent(logFile, 10000L, 1048576L);
    }

}
