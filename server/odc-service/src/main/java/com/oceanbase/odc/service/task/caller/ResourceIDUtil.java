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
package com.oceanbase.odc.service.task.caller;

import java.util.Map;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.resource.DefaultResourceOperatorBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * util to help build resource id from job entity and executor identifier
 * 
 * @author longpeng.zlp
 * @date 2024/9/2 11:58
 */
@Slf4j
public class ResourceIDUtil {
    public static final String REGION_PROP_NAME = "regionName";
    public static final String GROUP_PROP_NAME = "cloudProvider";
    public static final String DEFAULT_PROP_VALUE = "local";
    public static final String PROCESS_REGION_NAME = DEFAULT_PROP_VALUE;
    public static final String PROCESS_GROUP_NAME = "process";
    public static final ResourceLocation PROCESS_RESOURCE_LOCATION =
            new ResourceLocation(PROCESS_REGION_NAME, PROCESS_GROUP_NAME);

    /**
     * get with log is missing
     * 
     * @param jobParameters
     * @param propName
     * @return defaultValue if key is absent in jobParameters
     */
    public static String checkAndGetJobProperties(Map<String, String> jobParameters, String propName) {
        if (null == jobParameters) {
            throw new RuntimeException(
                    "get propName=" + propName + " failed from job context=" + jobParameters + " failed");
        }
        String ret = jobParameters.get(propName);
        if (null == ret) {
            throw new RuntimeException(
                    "get propName=" + propName + " failed from job context=" + jobParameters + " failed");
        } else {
            return ret;
        }
    }

    /**
     * resolve resourceID with default region and group name
     * 
     * @param executorIdentifier
     * @param jobProperties
     * @return
     */
    public static ResourceID getResourceID(ExecutorIdentifier executorIdentifier,
            Map<String, String> jobProperties) {
        return new ResourceID(getResourceLocation(jobProperties), DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE,
                executorIdentifier.getNamespace(),
                executorIdentifier.getExecutorName());
    }

    public static ResourceLocation getResourceLocation(Map<String, String> jobProperties) {
        String region = checkAndGetJobProperties(jobProperties, REGION_PROP_NAME);
        String group = checkAndGetJobProperties(jobProperties, GROUP_PROP_NAME);
        return new ResourceLocation(region, group);
    }

    /**
     * get resource id by jobEntity and executor identifier
     * 
     * @param executorIdentifier
     * @param jobEntity
     * @return
     */
    public static ResourceID getResourceID(ExecutorIdentifier executorIdentifier, JobEntity jobEntity) {
        return getResourceID(executorIdentifier, jobEntity.getJobProperties());
    }
}
