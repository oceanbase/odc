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

package com.oceanbase.odc.service.datatransfer.task;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import com.google.common.base.Verify;
import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferCallable;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus.Status;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datatransfer.DataTransferAdapter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class DataTransferTask implements Callable<DataTransferTaskResult> {
    private static final Set<String> OUTPUT_FILTER_FILES = new HashSet<>();

    private final DataMaskingService maskingService;
    @Getter
    private final Holder<DataTransferCallable> jobHolder;
    private final User creator;
    private final DataTransferConfig config;
    private final DataTransferAdapter adapter;
    private final DataTransferProperties properties;
    private final File workingDir;
    private final File logDir;

    @Override
    public DataTransferTaskResult call() throws Exception {
        try {
            SecurityContextUtils.setCurrentUser(creator);

            preHandle();

            DataTransferCallable job = TaskPluginUtil
                    .getDataTransferExtension(config.getConnectionInfo().getConnectType().getDialectType())
                    .generate(config, workingDir, logDir);
            jobHolder.setValue(job);

            DataTransferTaskResult result = job.call();

            validateSuccessful(result);

            postHandle(result);

            return result;

        } catch (Exception e) {
            log.warn("Failed to run data transfer task.", e);
            throw e;

        } finally {
            SecurityContextUtils.clear();
        }
    }

    private void preHandle() throws Exception {
        config.setUsePrepStmts(properties.isUseServerPrepStmts());
    }

    private void postHandle(DataTransferTaskResult result) throws Exception {

    }

    private void validateSuccessful(DataTransferTaskResult result) {
        List<String> failedObjects = ListUtils.union(result.getDataObjectsInfo(), result.getSchemaObjectsInfo())
                .stream()
                .filter(objectStatus -> objectStatus.getStatus() != Status.SUCCESS)
                .map(ObjectStatus::getSummary)
                .collect(Collectors.toList());
        Verify.verify(CollectionUtils.isEmpty(failedObjects),
                "Data transfer task completed with unfinished objects! Details : " + failedObjects);
    }
}
