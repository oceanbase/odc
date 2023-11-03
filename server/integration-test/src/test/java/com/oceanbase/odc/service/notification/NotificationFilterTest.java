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

package com.oceanbase.odc.service.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.service.notification.helper.NotificationPolicyFilter;
import com.oceanbase.odc.service.notification.model.EventLabels;

public class NotificationFilterTest {

    @Test
    public void testFilterNotificationPolicies_Succeed() {
        EventLabels labels = new EventLabels();
        labels.put("action", "failed");

        List<NotificationPolicyEntity> filtered = NotificationPolicyFilter.filter(labels, getPolicies());
        Assert.assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterNotificationPolicies_Fail() {
        EventLabels labels = new EventLabels();
        labels.put("action", "succeed");

        List<NotificationPolicyEntity> filtered = NotificationPolicyFilter.filter(labels, getPolicies());
        Assert.assertEquals(0, filtered.size());
    }

    List<NotificationPolicyEntity> getPolicies() {
        Map<String, String> conditions = new HashMap<>();
        conditions.put("action", "failed");
        NotificationPolicyEntity policy = new NotificationPolicyEntity();
        policy.setMatchExpression(JsonUtils.toJson(conditions));
        return Collections.singletonList(policy);
    }

}
