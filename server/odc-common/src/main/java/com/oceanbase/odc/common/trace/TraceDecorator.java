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
package com.oceanbase.odc.common.trace;

import java.util.Map;

import org.springframework.core.task.TaskDecorator;

/**
 * Runnable/Callable的装饰类，用于向子线程传递traceId
 *
 * @author zhigang.xzg
 * @date 2019/12/24
 */
public class TraceDecorator<V> implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Right now we are inside the Web thread context !
        // (Grab the current thread context data)
        Map<String, String> context = TraceContextHolder.getTraceContext();
        return () -> {
            try {
                // Right now we are inside @Async thread context !
                // (Restore the Web thread context data)
                TraceContextHolder.span(context);
                runnable.run();
            } finally {
                TraceContextHolder.clear();
            }
        };
    }

}
