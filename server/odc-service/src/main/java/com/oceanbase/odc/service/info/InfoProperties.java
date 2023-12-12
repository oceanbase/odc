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
package com.oceanbase.odc.service.info;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/1/5 下午2:05
 * @Description: [ODC basic info from table config_system_configuration]
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "odc.system.info")
@Data
public class InfoProperties {

    /**
     * 用户支持反馈邮箱
     */
    private String supportEmail;

    /**
     * 用户支持 URL 地址
     */
    private String supportUrl;

    /**
     * 首页文案
     */
    private String homePageText;

}
