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

import java.util.Optional;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.common.response.SuccessResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class ExecutorBizImpl implements ExecutorBiz {

    private static final String LOG_PATH_PATTERN = "%s/%s/task.%s";
    private final String logFilePrefix;

    public ExecutorBizImpl() {
        logFilePrefix = Optional.of(SystemUtils.getEnvOrProperty("odc.log.directory")).orElse("log");
    }

    @Override
    public SuccessResponse<String> log(Long id, String logType) {
        String filePath = String.format(LOG_PATH_PATTERN, logFilePrefix, id, logType);
        log.info("accept log request id = {}, logType= {}", id, logType);

        return new SuccessResponse<>(LogUtils.getLog(filePath));
    }
}
