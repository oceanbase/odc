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
import com.oceanbase.odc.service.resource.k8s.DefaultResourceOperatorBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * util to help build resource id from job entity and executor identifier
 * 
 * @author longpeng.zlp
 * @date 2024/9/2 11:58
 */
@Slf4j
public class ResourceIDUtil {
    public static final String DEFAULT_REGION_PROP_NAME = "region";
    public static final String DEFAULT_GROUP_PROP_NAME = "cloudProvider";
    public static final String DEFAULT_PROP_VALUE = "local";

    /**
     * get with log is missing
     * 
     * @param jobParameters
     * @param propName
     * @param defaultValue
     * @return defaultValue if key is absent in jobParameters
     */
    public static String checkAndGetJobProperties(Map<String, String> jobParameters, String propName,
            String defaultValue) {
        if (null == jobParameters) {
            log.warn("get propName={} failed from job context={} failed, use default value={}", propName, jobParameters,
                    defaultValue);
            return defaultValue;
        }
        String ret = jobParameters.get(propName);
        if (null == ret) {
            log.warn("get propName={} failed from job context={} failed, use default value={}", propName, jobParameters,
                    defaultValue);
            return defaultValue;
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
        String region = checkAndGetJobProperties(jobProperties, DEFAULT_REGION_PROP_NAME, DEFAULT_PROP_VALUE);
        String group = checkAndGetJobProperties(jobProperties, DEFAULT_GROUP_PROP_NAME, DEFAULT_PROP_VALUE);
        return new ResourceID(new ResourceLocation(region, group), DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE,
                executorIdentifier.getNamespace(),
                executorIdentifier.getExecutorName());
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
