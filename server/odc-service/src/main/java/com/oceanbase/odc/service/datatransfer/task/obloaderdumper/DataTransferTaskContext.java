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
package com.oceanbase.odc.service.datatransfer.task.obloaderdumper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus;
import com.oceanbase.tools.loaddump.common.model.Summary;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link DataTransferTaskContext}
 *
 * @author yh263208
 * @date 2022-07-05 17:50
 * @since ODC_release_3.4.0
 */
@Getter
public class DataTransferTaskContext implements Future<DataTransferTaskResult> {

    @Getter(AccessLevel.NONE)
    private final Future<DataTransferTaskResult> handle;
    volatile TaskContext dataContext;
    volatile TaskContext schemaContext;

    DataTransferTaskContext(@NonNull Future<DataTransferTaskResult> handle) {
        this.handle = handle;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return handle.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return handle.isCancelled();
    }

    @Override
    public boolean isDone() {
        return handle.isDone();
    }

    @Override
    public DataTransferTaskResult get() throws InterruptedException, ExecutionException {
        return handle.get();
    }

    @Override
    public DataTransferTaskResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return handle.get(timeout, unit);
    }

    public double getProgress() {
        double returnVal = 0.0;
        int totalTaskCount = 0;
        if (this.schemaContext != null) {
            totalTaskCount++;
            returnVal += this.schemaContext.getProgress().getProgress();
        }
        if (this.dataContext != null) {
            totalTaskCount++;
            returnVal += this.dataContext.getProgress().getProgress();
        }
        if (totalTaskCount <= 0) {
            return 0.0;
        }
        return returnVal / totalTaskCount;
    }

    public List<ObjectStatus> getSchemaObjectsInfo() {
        if (schemaContext == null) {
            return Collections.emptyList();
        }
        Summary summary = schemaContext.getSummary();
        Verify.notNull(summary, "Schema summary");
        /**
         * 导入导出组件在某些场景下（例如停止任务），会出现 {@link ObjectStatus#getName()} 为 {@code null}
         * 的场景，这会造成前端显示错误。在这里过滤掉这个异常值。
         */
        return summary.getObjectStatusList().stream().filter(s -> s.getName() != null)
                .peek(status -> {
                    if (StringUtils.isNotBlank(status.getType())) {
                        status.setType(ObjectType.valueOfName(status.getType()).name());
                    }
                }).collect(Collectors.toList());
    }

    public List<ObjectStatus> getDataObjectsInfo() {
        if (dataContext == null) {
            return Collections.emptyList();
        }
        Summary summary = dataContext.getSummary();
        Verify.notNull(summary, "Data summary");
        return summary.getObjectStatusList().stream().filter(s -> s.getName() != null)
                .peek(status -> {
                    if (StringUtils.isNotBlank(status.getType())) {
                        status.setType(ObjectType.valueOfName(status.getType()).name());
                    }
                }).collect(Collectors.toList());
    }

}
