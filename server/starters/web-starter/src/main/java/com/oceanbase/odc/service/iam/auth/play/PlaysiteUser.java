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
package com.oceanbase.odc.service.iam.auth.play;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2021/12/18 下午4:26
 * @Description: [Response of acquiring ob official website user, see:
 *               https://opendocs.alipay.com/pre-apis/02av0m]
 */
@Data
public class PlaysiteUser {
    /**
     * 自然实体类型。MEMBER（个人账号）、MASTER（企业主账号）
     */
    private String roleType;

    /**
     * 通行证ID, 如：20200007800854
     */
    private String passportId;

    /**
     * 自然实体id。当获取个人信息时为空，当获取企业信息时是企业id。如：20200007800000
     */
    private String entityId;


    public PlaysiteUser(String roleType, String passportId, String entityId) {
        this.roleType = roleType;
        this.passportId = passportId;
        this.entityId = entityId;
    }
}
