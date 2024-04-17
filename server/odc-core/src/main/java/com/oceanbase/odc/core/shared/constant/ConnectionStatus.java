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
package com.oceanbase.odc.core.shared.constant;

/**
 * 连接状态，注意不是 Session 状态
 */
public enum ConnectionStatus {
    /**
     * 可连接
     */
    ACTIVE,
    /**
     * 不可连接
     */
    INACTIVE,
    /**
     * 检测中
     */
    TESTING,
    /**
     * 连接已禁用，不进行检测
     */
    DISABLED,
    /**
     * 未知
     */
    UNKNOWN,

}
