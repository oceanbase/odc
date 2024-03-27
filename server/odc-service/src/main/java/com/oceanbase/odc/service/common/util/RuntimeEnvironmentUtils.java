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
package com.oceanbase.odc.service.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author jingtian
 * @date 2024/3/26
 */
@Component
public class RuntimeEnvironmentUtils {
    @Autowired
    private Environment environment;

    public Boolean isOBCloudRuntimeMode() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equals("alipay")) {
                String property = environment.getProperty("odc.iam.auth.type");
                if ("obcloud".equals(property)) {
                    return true;
                }
            }
        }
        return false;
    }
}
