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
package com.oceanbase.tools.dbbrowser.parser;

import java.util.Objects;

import lombok.NonNull;

class CacheElement<T> {

    private final T value;
    private final Exception exception;
    private final StackTraceElement[] traceElements;

    public CacheElement(@NonNull T value) {
        this.exception = null;
        this.value = value;
        this.traceElements = null;
    }

    public CacheElement(@NonNull Exception exception) {
        this.value = null;
        this.exception = exception;
        this.traceElements = getRealStackTraceElements(exception);
    }

    public synchronized T get() {
        if (this.exception != null) {
            Exception target = setStackTraceElements(this.exception);
            if (this.exception instanceof RuntimeException) {
                throw (RuntimeException) target;
            } else {
                throw new RuntimeException(target);
            }
        }
        return this.value;
    }

    private StackTraceElement[] getRealStackTraceElements(Exception e) {
        StackTraceElement elt = Thread.currentThread().getStackTrace()[3];
        int i;
        for (i = 0; i < e.getStackTrace().length; i++) {
            StackTraceElement item = e.getStackTrace()[i];
            if (Objects.equals(item.getClassName(), elt.getClassName())
                    && Objects.equals(item.getMethodName(), elt.getMethodName())) {
                i++;
                break;
            }
        }
        StackTraceElement[] target = new StackTraceElement[i];
        System.arraycopy(e.getStackTrace(), 0, target, 0, i);
        return target;
    }

    private Exception setStackTraceElements(Exception e) {
        StackTraceElement[] elts = Thread.currentThread().getStackTrace();
        int length = this.traceElements.length;
        StackTraceElement[] target = new StackTraceElement[length + elts.length - 4];
        System.arraycopy(this.traceElements, 0, target, 0, length);
        System.arraycopy(elts, 4, target, length, elts.length - 4);
        e.setStackTrace(target);
        return e;
    }

}
