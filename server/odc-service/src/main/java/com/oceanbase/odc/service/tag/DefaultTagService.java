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
package com.oceanbase.odc.service.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DefaultTagService implements TagService {

    private static final String TAG_SERVICE_CONFIG_KEY = "odc.tag.config";

    private final SystemConfigService systemConfigService;

    public DefaultTagService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public List<String> getTags(Long userId, String label) {
        Configuration configuration = systemConfigService.queryCacheByKey(TAG_SERVICE_CONFIG_KEY);
        if (configuration == null) {
            return Collections.emptyList();
        }
        List<UserTagItem> userTagItems = JsonUtils.fromJsonList(configuration.getValue(), UserTagItem.class);
        return MoreObjects.firstNonNull(userTagItems, new ArrayList<UserTagItem>()).stream()
                .filter(item -> Objects.equals(userId, item.getUserId())
                        && Objects.equals(label, item.getLabelKey()))
                .map(UserTagItem::getLabelValue).collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserTagItem {
        private Long userId;
        private String labelKey;
        private String labelValue;
    }

}
