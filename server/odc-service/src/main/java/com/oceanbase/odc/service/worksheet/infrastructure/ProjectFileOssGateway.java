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
package com.oceanbase.odc.service.worksheet.infrastructure;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.CloudSecurityTokenService;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.WorksheetOssGateway;

import lombok.extern.slf4j.Slf4j;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Slf4j
@Component
public class ProjectFileOssGateway implements WorksheetOssGateway {
    private static final Long DEFAULT_TOKEN_DURATION_SECONDS = 15 * 60L;
    private static final Long MAX_TOKEN_DURATION_SECONDS = 60 * 60L;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final CloudSecurityTokenService cloudSecurityTokenService;
    private final ObjectStorageConfiguration objectStorageConfiguration;

    public ProjectFileOssGateway(CloudObjectStorageService cloudObjectStorageService,
            @Autowired @Qualifier("publicEndpointCloudClient") CloudSecurityTokenService cloudSecurityTokenService,
            CloudEnvConfigurations cloudEnvConfigurations) {
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.cloudSecurityTokenService = cloudSecurityTokenService;
        this.objectStorageConfiguration = cloudEnvConfigurations.getObjectStorageConfiguration();
    }

    @Override
    public String generateUploadUrl(Long durationSeconds) {

        return "";
    }

    @Override
    public void copyTo(String tempObjectKey, Path destination) {
        String toObjectName = CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.ODC_PROJECT_FILES_PREFIX, destination.getStandardPath());
        try {
            cloudObjectStorageService.copyTo(tempObjectKey, toObjectName);
        } catch (IOException ex) {
            log.warn("Failed to copy object to OSS, tempObjectKey={},destination={}", tempObjectKey,
                    destination, ex);
            throw new InternalServerError("Failed to put object onto OSS", ex);
        }
    }

    @Override
    public String getContent(String objectKey) {
        return "";
    }

    @Override
    public void batchDelete(Set<String> objectKeys) {

    }
}
