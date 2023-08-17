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
package com.oceanbase.odc.common.security;

import com.oceanbase.odc.libinjection.Libinjection;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2021/7/6 上午10:15
 * @Description: [The wrapper class of 3rd-party library libinjection]
 */
public class SqlInjectionDetector {

    public static boolean isSqlInjection(@NonNull String sql) {
        Libinjection libinjection = new Libinjection();
        return libinjection.libinjection_sqli(sql);
    }

    public static boolean isNotSqlInjection(@NonNull String sql) {
        Libinjection libinjection = new Libinjection();
        return !libinjection.libinjection_sqli(sql);
    }
}
