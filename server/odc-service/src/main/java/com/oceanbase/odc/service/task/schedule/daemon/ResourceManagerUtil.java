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
package com.oceanbase.odc.service.task.schedule.daemon;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;

/**
 * @author longpeng.zlp
 * @date 2024/8/15 17:54
 */
public class ResourceManagerUtil {
    public static void markResourceReleased(JobEntity jobEntity, String executorIdentifierStr,
            ResourceManager resourceManager) {
        // mark resource as released to let resource collector collect resource
        if (StringUtils.isNotEmpty(executorIdentifierStr)) {
            ExecutorIdentifier executorIdentifier = ExecutorIdentifierParser.parser(executorIdentifierStr);
            ResourceID resourceID = ResourceIDUtil.getResourceID(executorIdentifier, jobEntity);
            resourceManager.release(resourceID);
        }
    }
}
