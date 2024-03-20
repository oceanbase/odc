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
package com.oceanbase.odc.service.onlineschemachange.oms.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目状态
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Getter
@AllArgsConstructor
public enum OmsProjectStatusEnum {

    /**
     * 未启动
     */
    INIT,
    /**
     * 迁移中
     */
    RUNNING,
    /**
     * 暂停中
     */
    SUSPEND,
    /**
     * 失败
     */
    FAILED,
    /**
     * 已完成
     */
    FINISHED,
    /**
     * 释放中
     */
    RELEASING,
    /**
     * 已释放
     */
    RELEASED,
    /**
     * 已删除
     */
    DELETED,

    UNKNOWN;

    public boolean isProjectDestroyed() {
        return Arrays.asList(OmsProjectStatusEnum.DELETED, OmsProjectStatusEnum.RELEASED,
                OmsProjectStatusEnum.RELEASING).contains(this);
    }
}
