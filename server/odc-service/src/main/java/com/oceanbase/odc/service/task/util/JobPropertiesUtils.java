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
package com.oceanbase.odc.service.task.util;

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.common.util.MapUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.enums.TaskMonitorMode;

import lombok.NonNull;

public class JobPropertiesUtils {

    public static void setLabels(@NonNull Map<String, String> jobProperties, @NonNull Map<String, String> labels) {
        jobProperties.put("labels", MapUtils.formatKvString(labels));
    }

    public static Map<String, String> getLabels(@NonNull Map<String, String> jobProperties) {
        if (StringUtils.isBlank(jobProperties.get("labels"))) {
            return new HashMap<>();
        }
        return MapUtils.fromKvString(jobProperties.get("labels"));
    }

    public static void setCloudProvider(@NonNull Map<String, String> jobProperties,
            @NonNull CloudProvider cloudProvider) {
        jobProperties.put(ResourceIDUtil.GROUP_PROP_NAME, cloudProvider.toString());
    }

    public static void setDefaultCloudProvider(@NonNull Map<String, String> jobProperties) {
        jobProperties.put("cloudProvider", ResourceIDUtil.PROCESS_REGION_NAME);
    }

    public static CloudProvider getCloudProvider(@NonNull Map<String, String> jobProperties) {
        String cloudProvider = jobProperties.get(ResourceIDUtil.GROUP_PROP_NAME);
        return StringUtils.isBlank(cloudProvider) ? CloudProvider.NONE : CloudProvider.valueOf(cloudProvider);
    }

    public static void setRegionName(@NonNull Map<String, String> jobProperties, @NonNull String regionName) {
        jobProperties.put(ResourceIDUtil.REGION_PROP_NAME, regionName);
    }

    public static void setDefaultRegionName(@NonNull Map<String, String> jobProperties) {
        jobProperties.put("regionName", ResourceIDUtil.PROCESS_REGION_NAME);
    }

    public static String getRegionName(@NonNull Map<String, String> jobProperties) {
        return jobProperties.get(ResourceIDUtil.REGION_PROP_NAME);
    }

    public static void setMonitorMode(@NonNull Map<String, String> jobProperties,
            @NonNull TaskMonitorMode monitorMode) {
        jobProperties.put("monitorMode", monitorMode.toString());
    }

    public static TaskMonitorMode getMonitorMode(@NonNull Map<String, String> jobProperties) {
        String monitorMode = jobProperties.get("monitorMode");
        return StringUtils.isBlank(monitorMode) ? TaskMonitorMode.PUSH : TaskMonitorMode.valueOf(monitorMode);
    }

    public static void setExecutorListenPort(@NonNull Map<String, String> jobProperties, @NonNull Integer listenPort) {
        jobProperties.put("executorListenPort", listenPort.toString());
    }

    public static int getExecutorListenPort(@NonNull Map<String, String> jobProperties) {
        String executorListenPort = jobProperties.get("executorListenPort");
        return StringUtils.isBlank(executorListenPort) ? 0 : Integer.parseInt(executorListenPort);
    }
}
