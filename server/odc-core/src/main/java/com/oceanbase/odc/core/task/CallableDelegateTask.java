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
package com.oceanbase.odc.core.task;

import java.util.concurrent.Callable;

import com.oceanbase.odc.common.event.EventPublisher;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A proxy task encapsulated by {@link java.util.concurrent.Callable}, this task cannot be called
 * through its {@code #run} method, otherwise an error will occur
 *
 * @author yh263208
 * @date 2021-11-11 15:27
 * @since ODC_release_3.2.2
 * @see BaseDelegateTask
 */
@Slf4j
public class CallableDelegateTask<T> extends BaseDelegateTask implements Callable<T> {

    private final Callable<T> callable;

    public CallableDelegateTask(@NonNull EventPublisher eventPublisher, @NonNull Callable<T> callable) {
        super(eventPublisher);
        this.callable = callable;
    }

    @Override
    public T call() throws Exception {
        return doCall(this.callable);
    }

}

