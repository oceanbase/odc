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
package com.oceanbase.odc.service.dlm.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.common.util.StringUtils;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2023/5/10 20:20
 * @Descripition:
 */

@Data
public class DataArchiveTableConfig {

    private String tableName;

    private String targetTableName;

    private List<String> partitions = new LinkedList<>();

    // the sql condition such as "gmt_create < '2023-01-01'"
    private String conditionExpression;

    private String minKey;

    private String maxKey;

    private Map<String, String> partName2MinKey = new HashMap<>();

    private Map<String, String> partName2MaxKey = new HashMap<>();

    public String getTargetTableName() {
        return StringUtils.isEmpty(targetTableName) ? tableName : targetTableName;
    }
}
