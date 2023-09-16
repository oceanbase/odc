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
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus.Status;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.TransferTaskStatus;
import com.oceanbase.odc.service.datatransfer.DataTransferAdapter;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConstants;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;

import lombok.Getter;

public abstract class BaseTransferTaskRunner implements Callable<DataTransferTaskResult> {
    protected final DataTransferParameter parameter;
    @Getter
    private final Holder<DataTransferTask> jobHolder;
    protected final DataTransferAdapter adapter;

    protected BaseTransferTaskRunner(DataTransferParameter parameter, Holder<DataTransferTask> jobHolder,
            DataTransferAdapter adapter) {
        this.parameter = parameter;
        this.jobHolder = jobHolder;
        this.adapter = adapter;
    }

    @Override
    public DataTransferTaskResult call() throws Exception {
        TraceContextHolder.span(ImmutableMap.of(DataTransferConstants.LOG_PATH_NAME, parameter.getLogPath()));
        try {

            preHandle();

            DataTransferTask job = TaskPluginUtil
                    .getDataTransferExtension(parameter.getConnectionInfo().getConnectType().getDialectType())
                    .build(parameter);
            jobHolder.setValue(job);

            job.transfer();

            DataTransferTaskResult result = DataTransferTaskResult.of(job.getStatus());

            validSuccessful(result);

            postHandle(result);

            return result;

        } finally {
            TraceContextHolder.clear();
        }
    }

    abstract protected void preHandle() throws Exception;

    abstract protected void postHandle(DataTransferTaskResult result) throws Exception;

    private void validSuccessful(TransferTaskStatus result) {
        List<String> failedObjects = ListUtils.union(result.getDataObjectsInfo(), result.getSchemaObjectsInfo())
                .stream()
                .filter(objectStatus -> objectStatus.getStatus() != Status.SUCCESS)
                .map(ObjectStatus::getSummary)
                .collect(Collectors.toList());
        Verify.verify(CollectionUtils.isEmpty(failedObjects),
                "Data transfer task completed with unfinished objects! Details : " + failedObjects);
    }

}
