/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.connection.database.model;

import java.util.List;

import com.oceanbase.odc.core.shared.constant.ResourceRoleName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: GetDatabaseOwnerResp
 * Package: com.oceanbase.odc.service.connection.database.model
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/2/26 15:40
 * @Version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetDatabaseOwnerResp {

    private Long databaseId;

    private Long projectId;

    private List<Member> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {
        private Long id;

        private String accountName;

        private String name;
    }
}
