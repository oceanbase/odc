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

import org.springframework.boot.system.ApplicationPid;

/**
 * @author keyang
 * @date 2025/04/07
 * @since 4.3.3
 */
public class ApplicationPidLoader {
    public static final ApplicationPidLoader INSTANCE = new ApplicationPidLoader();
    private volatile String applicationPid = null;

    private ApplicationPidLoader() {}

    public String getApplicationPid() {
        if (applicationPid == null) {
            synchronized (this) {
                if (applicationPid == null) {
                    applicationPid = new ApplicationPid().toString();
                }
            }
        }
        return applicationPid;
    }
}
