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
package com.oceanbase.odc.common.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;

/**
 * 
 */
public abstract class RouteLogRunnable implements Runnable {
    protected static Logger log = LogManager.getLogger(RouteLogRunnable.class);
    private final String workSpace;
    private final String taskId;
    private final String fileName;

    public RouteLogRunnable(String workSpace, String taskId, String fileName) {
        this.workSpace = workSpace;
        this.taskId = taskId;
        this.fileName = fileName;
    }

    public abstract void doRun();


    @Override
    public void run() {
        MDC.put("taskId", taskId);
        MDC.put("workSpace", workSpace);
        MDC.put("fileName", fileName);
        try {
            doRun();
        } finally {
            MDC.remove("taskId");
            MDC.remove("workSpace");
            MDC.remove("fileName");
        }
    }
}
