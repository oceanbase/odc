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
package com.oceanbase.odc.service.objectstorage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.druid.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.common.FileChecker;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/24 下午8:16
 * @Description: [This class is responsible for handling local file downloading request.]
 */
@Slf4j
@Service
@SkipAuthorize("download url is unique and available for 1 minute")
public class LocalFileTransferService {
    @Autowired
    private TempId2ObjectMetaCache tempId2ObjectMetaCache;

    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private FileChecker fileChecker;

    @Value("${odc.flow.async.max-upload-file-count:500}")
    private int maxAsyncUploadFileCount = 500;

    @Value("${odc.flow.async.max-upload-file-total-size:#{256*1024*1024}}")
    private long maxAsyncUploadFileTotalSize = 256 * 1024 * 1024L;

    public ResponseEntity<InputStreamResource> download(String tempId) throws IOException {
        // 根据临时 ID 从缓存中获取 objectMetadata
        ObjectMetadata objectMetadata = tempId2ObjectMetaCache.get(tempId);
        PreConditions.validExists(ResourceType.ODC_FILE, "tempId", tempId, () -> Objects.nonNull(objectMetadata));

        // 根据 bucket 和 objectId 加载 storageObject
        StorageObject storageObject =
                objectStorageFacade.loadObject(objectMetadata.getBucketName(), objectMetadata.getObjectId());

        // 将临时 ID 失效
        tempId2ObjectMetaCache.remove(tempId);
        ResponseEntity<InputStreamResource> response;
        response = WebResponseUtils.getFileAttachmentResponseEntity(new InputStreamResource(storageObject.getContent()),
                objectMetadata.getObjectName());
        return response;
    }

    public List<ObjectMetadata> batchUpload(String bucketName, List<MultipartFile> files) {
        PreConditions.notBlank(bucketName, "bucketName");
        PreConditions.notEmpty(files, "files");
        /**
         * 不同的业务可以使用不同的 bucket，如异步任务的 bucket 为：async/{userId}
         */
        if (StringUtils.equals(bucketName, "async")) {
            PreConditions.lessThanOrEqualTo("file count", LimitMetric.FILE_COUNT, files.size(),
                    maxAsyncUploadFileCount);
            PreConditions.lessThanOrEqualTo("file size", LimitMetric.FILE_SIZE,
                    files.stream().mapToLong(MultipartFile::getSize).sum(), maxAsyncUploadFileTotalSize);
            return putObjects(files,
                    bucketName.concat(File.separator).concat(authenticationFacade.currentUserIdStr()));
        } else if (StringUtils.equals(bucketName, "ssl")) {
            files.forEach(file -> PreConditions.validFileSuffix(file.getOriginalFilename(), Arrays.asList("pem")));
            return putObjects(files, sslBucketName());
        } else {
            log.warn("Bad upload request");
            throw new BadRequestException("Bad upload request");
        }
    }

    private List<ObjectMetadata> putObjects(List<MultipartFile> files, String bucket) {
        List<ObjectMetadata> returnVal = Lists.newArrayList();
        objectStorageFacade.createBucketIfNotExists(bucket);
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            fileChecker.validateSuffix(filename);
            try (InputStream inputStream = file.getInputStream()) {
                ObjectMetadata objectMetadata =
                        objectStorageFacade.putObject(bucket, filename, file.getSize(), inputStream);
                returnVal.add(objectMetadata);
            } catch (IOException ex) {
                log.warn("put object failed, cause={}", ex.getMessage());
                throw new InternalServerError("put object failed", ex);
            }
        }
        return returnVal;
    }

    private String sslBucketName() {
        return "ssl".concat(File.separator).concat(authenticationFacade.currentUserIdStr());
    }
}
