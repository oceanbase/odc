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

package com.oceanbase.odc.service.task.executor;

import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class ExecutorBizImpl implements ExecutorBiz {

    @Override
    public SuccessResponse<String> log(Long id, String logType) {
        log.info("Accept log request, task id = {}, logType = {}", id, logType);
        OdcTaskLogLevel logTypeLevel = null;
        try {
            logTypeLevel = OdcTaskLogLevel.valueOf(logType);
        } catch (Exception e) {
            log.warn("logType {} is illegal.", logType);
            new SuccessResponse<>("logType " + logType + " is illegal.");
        }

        String logFile = LogUtils.getJobLogFileWithPath(id, logTypeLevel);
        return new SuccessResponse<>(LogUtils.getLogContent(logFile));
    }
}
