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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.schedule.DefaultScheduler;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * custom scheduler for mock task
 *
 * @author yh263208
 * @date 2021-04-27 11:30
 * @since ODC_release_2.4.1
 */
@Slf4j
public class CustomMockScheduler extends DefaultScheduler {

    private final Map<String, String> traceContext;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final OssTaskReferManager taskReferManager;

    public CustomMockScheduler(@NonNull Map<String, String> traceContext,
            @NonNull OssTaskReferManager taskReferManager,
            @NonNull CloudObjectStorageService cloudObjectStorageService) {
        this.traceContext = traceContext;
        this.taskReferManager = taskReferManager;
        this.cloudObjectStorageService = cloudObjectStorageService;
    }

    @Override
    protected void onSuccess(TableTaskContext context) {
        if (!this.cloudObjectStorageService.supported()
                || CollectionUtils.isEmpty(context.getOutputFiles())) {
            return;
        }
        File file = context.getOutputFiles().get(0);
        if (file == null || !file.exists()) {
            return;
        }
        TraceContextHolder.span(traceContext);
        log.info("Odc will upload mock data file to oss server");
        try {
            String objectName = cloudObjectStorageService.uploadTemp(file.getName(), file);
            taskReferManager.put(context.getTableTaskId(), objectName);
            log.info("Upload the data file to the oss successfully, objectName={}", objectName);
        } catch (IOException e) {
            log.warn("Fail to upload file to oss, fileName={}", file.getName(), e);
        } finally {
            if (file.delete()) {
                log.info("Temporary data file deleted successfully, filePath={}", file.getAbsolutePath());
            } else {
                log.warn("Failed to delete temporary data file, filePath={}", file.getAbsolutePath());
            }
            TraceContextHolder.clear();
        }
    }

    @Override
    protected void onFailure(TableTaskContext context, Throwable e) {}

    @Override
    protected Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max) {
        Set<Set<String>> set = new HashSet<>();
        set.add(groups);
        return set;
    }

}
