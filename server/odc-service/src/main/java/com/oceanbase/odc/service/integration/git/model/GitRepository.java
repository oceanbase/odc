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

package com.oceanbase.odc.service.integration.git.model;

import java.util.Date;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/29
 */
@Data
public class GitRepository extends GitProvider {
    private Long id;
    private Date createTime;
    private Date updateTime;
    private Long creatorId;
    private Long organizationId;
    private Long projectId;
    private String name;
    private String description;
    private String sshUrl;
    private String cloneUrl;
}
