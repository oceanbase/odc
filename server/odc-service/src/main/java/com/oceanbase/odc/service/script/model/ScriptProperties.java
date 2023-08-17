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
package com.oceanbase.odc.service.script.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/3/23 下午8:41
 * @Description: []
 */
@Configuration
@RefreshScope
@Data
public class ScriptProperties {
    /**
     * 脚本查看/编辑内容的最大长度，单位为字节，默认 20 MB
     */
    @Value("${odc.script.max-edit-length:#{20*1024*1024}}")
    private long maxEditLength;

    /**
     * 脚本上传的最大长度，单位为字节，默认 250 MB
     */
    @Value("${odc.script.max-upload-length:#{250*1024*1024}}")
    private long maxUploadLength;
}
