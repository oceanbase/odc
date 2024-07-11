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
package com.oceanbase.odc.common.util;

import java.util.function.Supplier;

import lombok.NonNull;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/11
 */
public class LazyInitObject<T> {
    private static final Object NO_INIT = new Object();

    private volatile T target;
    private final Supplier<T> supplier;

    public LazyInitObject(@NonNull Supplier<T> supplier) {
        target = (T) NO_INIT;
        this.supplier = supplier;
    }

    public T get() {
        if (target == NO_INIT) {
            synchronized (this) {
                if (target == NO_INIT) {
                    target = supplier.get();
                }
            }
        }
        return target;
    }

}
