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
package com.oceanbase.odc.service.notification.helper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.notification.constant.EventLabelKeys;
import com.oceanbase.odc.service.notification.model.EventLabels;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 17:05
 * @Description: []
 */
public class EventUtils {
    public static String generateMatchExpression(Map<String, String> map) {
        if (CollectionUtils.isEmpty(map)) {
            return "[]";
        }
        return JsonUtils.toJson(map);
    }

    public static EventLabels buildEventLabels(TaskType taskType, String action, Long connectionId) {
        Map<String, String> labels = new HashMap<>();
        labels.put(EventLabelKeys.IDENTIFIER_KEY_TASK_TYPE, taskType.name());
        labels.put(EventLabelKeys.IDENTIFIER_KEY_ACTION, action);
        labels.put(EventLabelKeys.IDENTIFIER_KEY_CONNECTION_ID, String.valueOf(connectionId));
        return new EventLabels().addLabels(labels);
    }
}
