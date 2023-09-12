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

package com.oceanbase.odc.service.datatransfer.task.common;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import com.oceanbase.odc.service.datatransfer.task.TransferTask;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataTransferTaskRunner implements Callable<Void> {

    @Getter
    private final List<TransferTask> taskQueue;
    private final boolean isStopWhenError;
    @Getter
    private TransferTask current;

    public DataTransferTaskRunner(List<TransferTask> taskQueue, boolean isStopWhenError) {
        this.taskQueue = taskQueue;
        this.isStopWhenError = isStopWhenError;
    }

    @Override
    public Void call() throws Exception {
        Iterator<TransferTask> iterator = taskQueue.iterator();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                while (iterator.hasNext()) {
                    current = iterator.next();
                    try {

                        current.init();

                        current.transfer();

                    } catch (Exception ex) {
                        log.warn(String.format("Error occurred when transferring, reason:%s", ex.getMessage()));
                        current.fail(ex);

                        if (isStopWhenError) {
                            throw ex;
                        }
                    } finally {
                        current.destroyQuietly();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to execute data transfer task", ex);

            if (current != null) {
                current.destroyQuietly();
            }
            throw ex;
        }

        return null;
    }
}
