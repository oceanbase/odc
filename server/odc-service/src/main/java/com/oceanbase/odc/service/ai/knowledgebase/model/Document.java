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
package com.oceanbase.odc.service.ai.knowledgebase.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/21
 */
@Data
public class Document {

    /**
     * 文档内容
     */
    String pageContent;
    /**
     * 元数据信息
     */
    Map<String, String> metadata;
    /**
     * 得分
     */
    Double score;

    public String getOriginType() {
        return metadata.get("originType");
    }

    public void setOriginType(String originType) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("originType", originType);
    }

    public String getTableName() {
        return metadata == null ? null : metadata.get("tableName");
    }

    public void setTableName(String tableName) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("tableName", tableName);
    }

}
