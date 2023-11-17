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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.service.notification.model.EventLabels;

public class NotificationPolicyFilter {

    public static List<NotificationPolicyEntity> filter(EventLabels labels, List<NotificationPolicyEntity> policies) {
        List<NotificationPolicyEntity> filtered = new ArrayList<>();
        if (CollectionUtils.isEmpty(policies) || MapUtils.isEmpty(labels)) {
            return filtered;
        }
        for (NotificationPolicyEntity policy : policies) {
            Map<String, String> conditions =
                    JsonUtils.fromJsonMap(policy.getMatchExpression(), String.class, String.class);
            if (conditions.entrySet().stream().allMatch(entry -> labels.containsKey(entry.getKey())
                    && StringUtils.equals(entry.getValue(), labels.get(entry.getKey())))) {
                filtered.add(policy);
            }
        }
        return filtered;
    }

}
