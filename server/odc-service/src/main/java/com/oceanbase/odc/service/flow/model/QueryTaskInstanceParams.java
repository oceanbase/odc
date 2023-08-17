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
package com.oceanbase.odc.service.flow.model;

import java.util.Date;
import java.util.List;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.Builder;
import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/2/14
 */

@Data
@Builder
public class QueryTaskInstanceParams {
    private static final String DATABASE_NAME_KEY = "databaseName";
    private static final String CREATOR_NAME_KEY = "creatorName";
    private static final String KEY_VALUE_SEPARATOR = ":";

    private List<Long> connectionIds;
    // eg: databaseName:xxx or creatorName:xxx
    private String fuzzySearchKeyword;
    private List<TaskStatus> statuses;
    private TaskType type;
    private Long id;
    private Date startTime;
    private Date endTime;
    private Boolean createdByCurrentUser;
    private Boolean approveByCurrentUser;

    public String getFuzzyDatabaseName() {
        if (StringUtils.isNotBlank(fuzzySearchKeyword)) {
            String[] kv = fuzzySearchKeyword.split(KEY_VALUE_SEPARATOR);
            if (kv.length == 2 && kv[0].equalsIgnoreCase(DATABASE_NAME_KEY)) {
                return kv[1];
            }
        }
        return null;
    }

    public String getFuzzyCreatorName() {
        if (StringUtils.isNotBlank(fuzzySearchKeyword)) {
            String[] kv = fuzzySearchKeyword.split(KEY_VALUE_SEPARATOR);
            if (kv.length == 2 && kv[0].equalsIgnoreCase(CREATOR_NAME_KEY)) {
                return kv[1];
            }
        }
        return null;
    }
}
