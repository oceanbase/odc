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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.util;

import java.util.concurrent.atomic.AtomicLong;

public class DataXJobIdProvider {

    private final AtomicLong currentJobId = new AtomicLong(1L);

    private static volatile DataXJobIdProvider instance;

    private DataXJobIdProvider() {}

    public static DataXJobIdProvider getInstance() {
        if (instance == null) {
            synchronized (DataXJobIdProvider.class) {
                instance = new DataXJobIdProvider();
            }
        }
        return instance;
    }

    public synchronized Long fetch() {
        return currentJobId.getAndIncrement();
    }

}
