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
package com.oceanbase.odc.metadb.ai;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/24
 */
@Data
public class SchemaKBVectorEntity {
    private Long id;
    private List<Float> embedding;
    private String content;
    private String contentSha1;
    private Long databaseId;
    private String tableName;
    private String tag;
    private long organizationId;
    private Double distance;
    private Date createTime;
    private Date updateTime;
}
