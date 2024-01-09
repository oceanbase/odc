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
package com.oceanbase.odc.service.objectstorage.cloud;

import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.amazonaws.util.SdkHttpUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadObjectTemporaryCredential;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize
public class CloudResourceService {
    private static final Long DEFAULT_TOKEN_DURATION_SECONDS = 15 * 60L;
    private static final int MAX_OBJECT_NAME_LENGTH = 1023;
    private final CloudObjectStorageService cloudObjectStorageService;
    private final CloudSecurityTokenService cloudSecurityTokenService;

    public CloudResourceService(CloudObjectStorageService cloudObjectStorageService,
            @Autowired @Qualifier("publicEndpointCloudClient") CloudSecurityTokenService cloudSecurityTokenService) {
        this.cloudObjectStorageService = cloudObjectStorageService;
        this.cloudSecurityTokenService = cloudSecurityTokenService;
    }

    public UploadObjectTemporaryCredential generateTempCredential(GenerateTempCredentialReq req) {
        Verify.verify(cloudObjectStorageService.supported(), "Cloud object storage not supported");
        String bucketName = cloudObjectStorageService.getBucketName();
        String objectName = CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.ODC_TRANSFER_PREFIX, req.getFileName());
        objectName = SdkHttpUtils.urlEncode(objectName, true);
        if (objectName.length() > MAX_OBJECT_NAME_LENGTH) {
            throw new InvalidFileFormatException(ErrorCodes.IllegalFileName, "The filename is too long!");
        }
        Long durationSeconds =
                Objects.nonNull(req.getDurationSeconds()) ? req.getDurationSeconds() : DEFAULT_TOKEN_DURATION_SECONDS;
        UploadObjectTemporaryCredential credential = cloudSecurityTokenService.generateTempCredential(bucketName,
                objectName, durationSeconds);
        credential.setBucket(bucketName);
        ObjectStorageConfiguration objectStorageConfiguration =
                cloudObjectStorageService.getObjectStorageConfiguration();
        credential.setEndpoint(objectStorageConfiguration.getPublicEndpoint());
        credential.setRegion(objectStorageConfiguration.getRegion());
        credential.setBucketEndpoint(bucketName + "." + objectStorageConfiguration.getPublicEndpoint());
        log.info("Generate temp credential, req={}, filePath={}", req, credential.getFilePath());
        return credential;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SkipAuthorize
    public static class GenerateTempCredentialReq {
        @NotBlank
        private String fileName;
        private Long durationSeconds;

        public static GenerateTempCredentialReq of(String fileName, Long durationSeconds) {
            GenerateTempCredentialReq req = new GenerateTempCredentialReq();
            req.setFileName(fileName);
            req.setDurationSeconds(durationSeconds);
            return req;
        }
    }
}
