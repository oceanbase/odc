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

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.CloudSecurityTokenService;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Slf4j
@Component
public class WorksheetObjectStorageGateway
        implements com.oceanbase.odc.service.worksheet.domain.WorksheetObjectStorageGateway {

    @Autowired
    private ObjectStorageFacade objectStorageFacade;



    @Override
    public String generateUploadUrl(String bucket, String objectId) {
        return objectStorageFacade.getDownloadUrl(bucket, objectId);
    }

    @Override
    public void copyTo(String tempObjectId, Path destinationPath) {}

    @Override
    public String getContent(String objectId) {
        return "";
    }

    @Override
    public void batchDelete(Set<String> objectIds) {

    }

    @Override
    public String generateDownloadUrl(String objectId) {
        return "";
    }

    @Override
    public void downloadToFile(String objectName, File toFile) {

    }

    @Override
    public String uploadFile(File file, int durationSeconds) {
        return "";
    }
}
