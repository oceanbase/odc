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
package com.oceanbase.odc.service.iam.util;

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.core.shared.constant.ResourceRoleName;

/**
 * @Author: Lebie
 * @Date: 2024/11/19 16:05
 * @Description: []
 */
public class GlobalResourceRoleUtil {
    public final static String GLOBAL_PROJECT_OWNER = "global_project_owner";
    public final static String GLOBAL_PROJECT_DBA = "global_project_dba";
    public final static String GLOBAL_PROJECT_SECURITY_ADMINISTRATOR = "global_project_security_administrator";
    private final static Map<String, ResourceRoleName> globalRoleName2ResourceRoleName = new HashMap<>();
    private final static Map<ResourceRoleName, String> resourceRoleName2GlobalRoleName = new HashMap<>();

    static {
        globalRoleName2ResourceRoleName.put(GLOBAL_PROJECT_OWNER, ResourceRoleName.OWNER);
        globalRoleName2ResourceRoleName.put(GLOBAL_PROJECT_DBA, ResourceRoleName.DBA);
        globalRoleName2ResourceRoleName.put(GLOBAL_PROJECT_SECURITY_ADMINISTRATOR,
                ResourceRoleName.SECURITY_ADMINISTRATOR);

        resourceRoleName2GlobalRoleName.put(ResourceRoleName.OWNER, GLOBAL_PROJECT_OWNER);
        resourceRoleName2GlobalRoleName.put(ResourceRoleName.DBA, GLOBAL_PROJECT_DBA);
        resourceRoleName2GlobalRoleName.put(ResourceRoleName.SECURITY_ADMINISTRATOR,
                GLOBAL_PROJECT_SECURITY_ADMINISTRATOR);
    }

    public static ResourceRoleName getResourceRoleName(String globalRoleName) {
        return globalRoleName2ResourceRoleName.get(globalRoleName);
    }

    public static String getGlobalRoleName(ResourceRoleName resourceRoleName) {
        return resourceRoleName2GlobalRoleName.get(resourceRoleName);
    }

}
