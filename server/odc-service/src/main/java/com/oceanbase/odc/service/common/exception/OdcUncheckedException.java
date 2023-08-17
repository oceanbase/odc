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
package com.oceanbase.odc.service.common.exception;

import org.apache.commons.lang3.Validate;

import lombok.Getter;

public class OdcUncheckedException extends RuntimeException {
    @Getter
    private final Exception checkedException;

    public OdcUncheckedException(Exception origin) {
        super(origin);
        Validate.notNull(origin, "origin exception require not null");
        Validate.isTrue(!(origin instanceof RuntimeException), "cannot wrap unchecked exception");
        this.checkedException = origin;
    }
}
