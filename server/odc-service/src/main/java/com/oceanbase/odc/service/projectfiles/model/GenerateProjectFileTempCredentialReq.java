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

package com.oceanbase.odc.service.projectfiles.model;

import javax.validation.constraints.Max;

import lombok.Data;

/**
 * 批量上传文件请求
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Data
public class GenerateProjectFileTempCredentialReq {
    @Max(value = 3600, message = "durationSeconds must be less than or equal to 3600")
    private Long durationSeconds;
}
