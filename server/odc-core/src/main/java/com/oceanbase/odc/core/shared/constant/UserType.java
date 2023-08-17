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
 * ODC 账号类型
 */
public enum UserType {
    /**
     * 系统账号，用于 ODC 内部的调用的身份识别，如 RPC 调用、内置资源创建等场景
     */
    SYSTEM,

    /**
     * 普通用户，通过账号密码登录到 ODC 执行操作
     */
    USER,

    /**
     * 服务类型，用于系统集成自动化调用的身份识别
     */
    SERVICE,

}
