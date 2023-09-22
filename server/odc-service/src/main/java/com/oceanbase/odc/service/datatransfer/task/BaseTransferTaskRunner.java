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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferTask;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus.Status;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.TransferTaskStatus;
import com.oceanbase.odc.service.datatransfer.DataTransferAdapter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferProperties;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseTransferTaskRunner implements Callable<DataTransferTaskResult> {
    @Getter
    private final Holder<DataTransferTask> jobHolder;
    private final User creator;
    protected final DataTransferParameter parameter;
    protected final DataTransferAdapter adapter;
    protected final DataTransferProperties properties;

    protected BaseTransferTaskRunner(DataTransferParameter parameter, Holder<DataTransferTask> jobHolder,
            @NonNull User creator, DataTransferAdapter adapter, DataTransferProperties properties) {
        this.parameter = parameter;
        this.jobHolder = jobHolder;
        this.creator = creator;
        this.adapter = adapter;
        this.properties = properties;
    }

    @Override
    public DataTransferTaskResult call() throws Exception {
        try {
            SecurityContextUtils.setCurrentUser(creator);

            preHandle();

            DataTransferTask job = TaskPluginUtil
                    .getDataTransferExtension(parameter.getConnectionInfo().getConnectType().getDialectType())
                    .build(parameter);
            jobHolder.setValue(job);

            TransferTaskStatus status = job.transfer();

            validateSuccessful(status);

            DataTransferTaskResult result = DataTransferTaskResult.of(status);

            postHandle(result);

            return result;

        } catch (Exception e) {
            log.warn("Failed to run data transfer task.", e);
            throw e;

        } finally {
            SecurityContextUtils.clear();
        }
    }

    protected void preHandle() throws Exception {
        parameter.setUsePrepStmts(properties.isUseServerPrepStmts());
    }

    abstract protected void postHandle(DataTransferTaskResult result) throws Exception;

    private void validateSuccessful(TransferTaskStatus result) {
        List<String> failedObjects = ListUtils.union(result.getDataObjectsInfo(), result.getSchemaObjectsInfo())
                .stream()
                .filter(objectStatus -> objectStatus.getStatus() != Status.SUCCESS)
                .map(ObjectStatus::getSummary)
                .collect(Collectors.toList());
        Verify.verify(CollectionUtils.isEmpty(failedObjects),
                "Data transfer task completed with unfinished objects! Details : " + failedObjects);
    }

}
