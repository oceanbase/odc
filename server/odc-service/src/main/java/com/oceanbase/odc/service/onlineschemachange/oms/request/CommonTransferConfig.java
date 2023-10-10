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
package com.oceanbase.odc.service.onlineschemachange.oms.request;

import lombok.Data;

/**
 * 创建项目公共配置
 * 
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class CommonTransferConfig {
    /**
     * 传输的表类型 ALL 所有表。默认 WITH_UNIQUE_ROW_ID 有唯一行标识的表。这类表满足满足如下条件中任一个： 1）有主键（primary key）
     * 2）没有主键，但是有唯一索引（unique index），且该唯一索引下所有字段都有 not null 约束。
     *
     * WITHOUT_UNIQUE_ROW_ID 没有唯一行标识的表。WITH_UNIQUE_ROW_ID 的反集。;
     */
    private String tableCategory = "ALL";
    /**
     * 是否双活场景
     */
    private Boolean activeActive = Boolean.FALSE;
    /**
     * 投递到消息队列时，数据 json 序列化类型 DEFAULT, 默认 CANAL, DATAWORKS_V2, SHAREPLEX, DEFAULT_WITH_SCHEMA, DEBEZIUM
     */
    private String mqSerializerType = "DEFAULT";
    /**
     * 投递到消息队列时，分区路由方式 指定分区投递 ONE, 根据 主键/分片列 值进行 hash 分区投递 HASH 根据库表名进行 hash 分区投递 TABLE, MD5,
     * ORIGINAL_VAL
     */
    private String mqPartitionMode = "HASH";

    /**
     * 投递到消息队列时，指定 topic 的类型 endpointType = DATAHUB 时生效 TUPLE 默认 Used to store structured data BLOB Used
     * to store unstructured data
     */
    private String datahubTopicType = "TUPLE";

    /**
     * 业务系统标识(可选)
     */
    private String dataWorksBusinessName = "ODC";

}
