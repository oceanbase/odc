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
package com.oceanbase.odc.service.connection.model;

/**
 * OceanBase 集群访问方式
 */
public enum OceanBaseAccessMode {
    /**
     * 直接访问, 私有云、阿里云专有云 使用
     */
    DIRECT,

    /**
     * 通过 IC 代理访问，OceanBase Cloud 使用
     */
    IC_PROXY,

    /**
     * 通过 VPC 地址访问，阿里云公有云 使用
     */
    VPC

}
