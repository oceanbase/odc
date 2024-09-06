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
package com.oceanbase.odc.service.config;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import cn.hutool.core.io.FileUtil;
import lombok.Data;

@ConfigurationProperties(prefix = "odc.web.log")
@Component
@Data
public class LoggerProperty {

    // This is the directory path for storing temporary log files from scheduled tasks pods
    private String tempScheduleTaskLogDir;

    private String directory = "./log";

    private Long maxLimitedCount = 10000L;

    // unit：B
    private Long maxSizeCount = 1024L * 1024;

    @PostConstruct
    public void init() {
        if (this.tempScheduleTaskLogDir == null) {
            this.tempScheduleTaskLogDir =
                    FileUtil.normalize(System.getProperty("user.dir") + File.separator + "log/running-job-temp-logs");
        }
    }
}
