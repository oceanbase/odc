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

import java.util.List;

import javax.validation.constraints.Null;

import org.hibernate.validator.constraints.Range;

import com.google.common.collect.Lists;
import com.oceanbase.odc.service.onlineschemachange.model.ThrottleConfig;

import lombok.Data;

/**
 * 创建项目增量传输配置
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class IncrTransferConfig implements ThrottleConfig {

    /**
     * 勾选了增量且没勾选全量时，允许设置增量起始位点，秒级时间戳 最多30天内，实际根据store情况
     */
    @Null
    private Long startTimestamp;
    /**
     * 有增量同步阶段时，同步的增量数据类型 <code>
     *  HEARTBEAT,
     *  INSERT,
     *  UPDATE,
     *  DELETE,
     *  BEGIN,
     *  COMMIT,
     *  ROLLBACK,
     *  DDL,
     *  ROW
     * </code>
     */
    private List<String> recordTypeWhiteList = Lists.newArrayList("INSERT", "UPDATE", "DELETE");
    /**
     * 有增量同步阶段，且增量日志拉取组件为 Store 时，日志保存时间, 单位小时，默认7天，最长365天
     */
    @Range(min = 1, max = 8760)
    private Integer storeLogKeptHour = 168;
    /**
     * 有增量同步阶段时，且增量日志拉取组件为 Store 时，是否开启事务内序号编排
     */
    private Boolean enableSequencingWithinTxn = Boolean.TRUE;
    /**
     * 增量同步线程数，默认64 1~1024 建议值：机器的核数*4
     */
    @Range(min = 1, max = 1024)
    private Integer incrSyncConcurrency = 64;
    /**
     * 是否开启增量记录(dml\ddl)统计
     */
    private Boolean enableIncrSyncStatistics = Boolean.FALSE;

    private Integer throttleRps;

    private Integer throttleIOPS;
}
