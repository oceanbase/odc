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

package com.oceanbase.odc.plugin.task.api.datatransfer;

import java.util.List;
import java.util.concurrent.Callable;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;

/**
 * @author liuyizhuo.lyz
 * @date 2023-09-15
 */
public interface DataTransferJob extends Callable<DataTransferTaskResult> {

    List<ObjectResult> getDataObjectsStatus();

    List<ObjectResult> getSchemaObjectsStatus();

    /**
     * @return current progress percentage
     */
    double getProgress();

    /**
     * cancel task
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns true if this task was cancelled before it completed normally.
     */
    boolean isCanceled();

}
