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
package com.oceanbase.odc.core.datamasking.exception;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */
public class CharNotInAlphabetException extends Exception {
    private static final long serialVersionUID = -4571521243330332306L;

    public CharNotInAlphabetException() {
        super();
    }

    public CharNotInAlphabetException(String message, Throwable cause) {
        super(message, cause);
    }

    public CharNotInAlphabetException(String message) {
        super(message);
    }

    public CharNotInAlphabetException(Throwable cause) {
        super(cause);
    }
}
