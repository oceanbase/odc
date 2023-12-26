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

import com.oceanbase.odc.service.connection.model.ConnectionConfig;

/**
 * @Author: Lebie
 * @Date: 2021/11/22 下午2:53
 * @Description: [Responsible for managing the lifecycle of result set export tasks]
 */
public interface ResultSetExportTaskManager {
    /**
     * start a result set export task
     *
     * @return ResultSetExportTaskInfo
     */
    ResultSetExportTaskContext start(ConnectionConfig connectionConfig, ResultSetExportTaskParameter taskReq,
            String taskId);

}
