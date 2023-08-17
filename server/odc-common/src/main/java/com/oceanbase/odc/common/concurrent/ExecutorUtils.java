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
package com.oceanbase.odc.common.concurrent;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : ExecutorUtils.java, v 0.1 2020-03-31 13:10
 */
@Slf4j
public class ExecutorUtils {

    public static void gracefulShutdown(ExecutorService executor, String executorName, long timeoutSeconds) {
        if (Objects.isNull(executor)) {
            return;
        }
        executor.shutdown();
        log.debug("shutdown signal received, terminating...");
        try {
            if (executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.info("executor {} terminated success", executorName);
            } else {
                log.warn("executor {} terminate failed, forcing shutdown...", executorName);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("executor {} terminate interrupted, message={}", executorName, e.getMessage());
        }
    }
}
