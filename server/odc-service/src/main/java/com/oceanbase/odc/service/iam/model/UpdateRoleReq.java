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

import java.util.List;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/8/9
 */

@Data
public class UpdateRoleReq extends CreateRoleReq {
    // for binding or unbinding users when updating role
    private List<Long> bindUserIds;
    private List<Long> unbindUserIds;
}
