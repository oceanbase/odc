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
package com.oceanbase.odc.service.onlineschemachange.oms.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import lombok.Data;

/**
 * 全量迁移步骤信息
 *
 * @author yaobin
 * @date 2023-06-08
 * @since 4.2.0
 */
@Data
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class FullTransferStepInfoVO extends BaseProjectStepInfoVO {
    /**
     * 已完成迁移行数/实际行数。
     */
    private Long processedRecords;
    /**
     * 预估总行数。
     */
    private Long capacity;
    /**
     * 源端读取 rps，单位：记录数/秒。
     */
    private Long srcRps;

    /**
     * 源端读取 rt，单位：毫秒/条。
     */
    private Long srcRt;
    /**
     * 源端读取流量，单位：字节数/秒。
     */
    private Long srcIops;
    /**
     * 目标端读取/写入 rps，单位：记录数/秒。
     */
    private Long dstRps;
    /**
     * 目标端读取/写入 rt，单位：毫秒/条。
     */
    private Long dstRt;
    /**
     * 目标端读取/写入流量，单位：字节数/秒。
     */
    private Long dstIops;
    /**
     * 源端读取 rps 基准值。
     */
    private Long srcRpsRef;
    /**
     * 源端读取 rt 基准值。
     */
    private Long srcRtRef;
    /**
     * 目标端读取/写入 rps 基准值。
     */
    private Long dstRpsRef;

    /**
     * 目标端读取/写入 rt 基准值。
     */
    private Long dstRtRef;

    /**
     * 源端读取流量，基准值。
     */
    private Long srcIopsRef;

}
