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
package com.oceanbase.odc.service.flow.task.model;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NonNull;

@Data
@RefreshScope
@Configuration
public class MockProperties {

    @Value("${odc.task.mockData.maxPoolSize:6}")
    private int maxPoolSize = 6;
    @Value("${odc.task.mockData.concurrent:1}")
    private int concurrent = 1;
    @Value("${odc.task.mockData.maxRowCount:10000000}")
    private long maxRowCount = 10000000;
    /**
     * 50 MB
     */
    @Value("${odc.task.mockData.maxSingleFileSizeInBytes:52428800}")
    private Long maxSingleFileSizeInBytes = 52428800L;
    /**
     * 400 MB
     */
    @Value("${odc.task.mockData.maxFileOutputSizeInBytes:419430400}")
    private Long maxFileOutputSizeInBytes = 419430400L;
    public String resultFileLocationPrefix = "./data/data_mocker";

    public File getDownloadPath(@NonNull String taskId) {
        return new File(String.format("%s/%s/download", this.resultFileLocationPrefix, taskId));
    }

}
