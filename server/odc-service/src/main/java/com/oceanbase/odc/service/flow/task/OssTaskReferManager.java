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
package com.oceanbase.odc.service.flow.task;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.NonNull;

/**
 * Cache object for oss
 *
 * @author yh263208
 * @date 2021-12-15 10:44
 * @since ODC_release_3.2.3
 */
@Component
public class OssTaskReferManager {

    private final Map<String, String> taskId2ObjectName = new ConcurrentHashMap<>();

    public void put(@NonNull String taskId, @NonNull String objectName) {
        taskId2ObjectName.put(taskId, objectName);
    }

    public void remove(String objectName) {
        Set<Entry<String, String>> entrySet = taskId2ObjectName.entrySet();
        entrySet.removeIf(entry -> Objects.equals(entry.getValue(), objectName));
    }

    public String get(@NonNull String taskId) {
        return taskId2ObjectName.get(taskId);
    }

}
