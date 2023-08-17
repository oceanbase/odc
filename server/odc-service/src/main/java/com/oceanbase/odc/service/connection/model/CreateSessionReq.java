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

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.common.json.SensitiveInput;

import lombok.Data;
import lombok.ToString;

/**
 * @author yizhou.xw
 * @version : CreateSessionReq.java, v 0.1 2021-07-21 5:47 PM
 */
@Data
@ToString(exclude = {"password"})
public class CreateSessionReq {
    /**
     * 连接ID，对应 /api/v1 的 sid，注意之前是 String 类型，现在统一为 Long
     */
    @NotNull
    private Long connectionId;
    /**
     * 连接密码，对于公共连接无需提供，对于个人连接如果未保存密码则需要提供
     */
    @SensitiveInput
    private String password;
    /**
     * 可以根据已有session创建一个会话，主要的应用场景是不保存连接密码的连接 当该字段不为null时将忽略{@code connectionId}和{@code password}，完全
     * 以该字段为准
     */
    private String copiedFromSessionId;
}
