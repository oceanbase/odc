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
 * 创建项目 全量传输配置
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class FullTransferConfig {

    /**
     * 在处理源端无唯一索引表德的全量迁移时，是否 truncate 目标表（清空目标表数据）,组件层面默认为 true 目前场景：PolarDB-X 1.0
     * 多链路无主键表汇聚到同一目标端表，避免前若干条链路前有部分数据后，另一链路刚启动就清空了历史的迁移数据
     */
    private Boolean nonePkUkTruncateDstTable = Boolean.TRUE;
    /**
     * 允许目标端表非空，在源端多个分表向目标端一张表聚合迁移时，需要打开 注意：只有在用户没有选中"全量校验"的情况下，才允许打开此开关。
     */
    private Boolean allowDestTableNotEmpty = Boolean.TRUE;

    /**
     * 全量迁移 配置(STEADY：平稳，NORMAL：正常，FAST：快速)。
     */
    private String fullTransferSpeedMode = "STEADY";

    /**
     * 全量校验 配置(STEADY：平稳，NORMAL：正常，FAST：快速)。
     */
    private String fullVerifySpeedMode = "STEADY";
}
