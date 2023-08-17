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
package com.oceanbase.odc.service.iam.model;

import java.security.Principal;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.shared.constant.Cipher;

import lombok.Data;
import lombok.ToString;

/**
 * @author kuiseng.zhb
 *
 *         已过时，该类目前仅在数据迁移中使用，业务层不要使用该类。
 */
@Deprecated
@Data
@ToString(exclude = {"password"})
public class UserInfo implements Principal {

    private long id;
    private String name;
    private String email;

    /**
     * 避免密码字段回传
     */
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private String role;
    private int status = 1;
    private String desc;
    private Timestamp gmtCreated;
    private Timestamp gmtModified;
    private boolean enabled = true;

    @JsonIgnore
    private Cipher cipher;

}
