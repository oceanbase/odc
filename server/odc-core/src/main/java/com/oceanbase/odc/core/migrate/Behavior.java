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
package com.oceanbase.odc.core.migrate;

import org.apache.commons.lang3.StringUtils;

/**
 * the VERSIONED script will be executed ordered, <br>
 * the REPEATABLE script will be executed after VERSIONED ones
 * 
 * @author yizhou.xw
 * @version : Behavior.java, v 0.1 2021-03-26 14:07
 */
public enum Behavior {
    /**
     * the script belong to a specific version
     */
    VERSIONED,

    /**
     * the script can be executed repeatable
     */
    REPEATABLE,

    /**
     * unknown behavior
     */
    UNKNOWN,
    ;

    public static Behavior fromAnnotation(Migratable migrate) {
        return migrate.repeatable() ? REPEATABLE : VERSIONED;
    }

    public static Behavior fromFileName(String fileName) {
        if (StringUtils.startsWith(fileName, Constants.VERSIONED_PREFIX)) {
            return VERSIONED;
        }
        if (StringUtils.startsWith(fileName, Constants.REPEATABLE_PREFIX)) {
            return REPEATABLE;
        }
        return UNKNOWN;
    }
}
