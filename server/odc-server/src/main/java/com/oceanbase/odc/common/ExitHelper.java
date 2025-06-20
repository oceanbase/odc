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
package com.oceanbase.odc.common;

import java.util.concurrent.CountDownLatch;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class ExitHelper {

    private static final CountDownLatch LATCH = new CountDownLatch(1);

    public static void await() {
        try {
            LATCH.await();
        } catch (InterruptedException e) {
            log.warn("Await thread be interrupted and exit:", e);
        }
    }

    public static void notifyFinished() {
        LATCH.countDown();
    }
}
