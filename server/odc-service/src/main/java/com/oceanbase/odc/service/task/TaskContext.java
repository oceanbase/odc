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
package com.oceanbase.odc.service.task;

import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.caller.JobContext;

/**
 * context for task runtime
 * 
 * @author longpeng.zlp
 * @date 2024/10/10 17:39
 */
public interface TaskContext {
    /**
     * provide exception listener
     * 
     * @return
     */
    ExceptionListener getExceptionListener();

    /**
     * provide job context
     * 
     * @return
     */
    JobContext getJobContext();

    /**
     * get shared storage for task upload or download file
     *
     * @return
     */
    CloudObjectStorageService getSharedStorage();
}
