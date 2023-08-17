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
package com.oceanbase.odc.core.sql.execute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;

/**
 * {@link FutureResult}
 *
 * @author yh263208
 * @date 2021-11-19 16:04
 * @since ODC_release_3.2.2
 */
public class FutureResult<T> implements Future<T> {

    private final T returnVal;

    private FutureResult(@NonNull T value) {
        this.returnVal = value;
    }

    public static <V> Future<V> successResult(@NonNull V result) {
        return new FutureResult<>(result);
    }

    public static <V> Future<List<V>> successResultList(@NonNull V result) {
        return new FutureResult<>(Collections.singletonList(result));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() {
        return this.returnVal;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        return this.returnVal;
    }

}
