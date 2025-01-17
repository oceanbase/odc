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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.oceanbase.odc.service.objectstorage.cloud.client.CloudException;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.CopyObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.GetObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectSummary;
import com.oceanbase.odc.service.objectstorage.cloud.model.PutObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;

/**
 * Cloud object storage
 */
public interface CloudObjectStorage {
    /**
     * if cloud object storage supported
     */
    boolean supported();

    /**
     * Get geographic region
     */
    String getBucketLocation(String bucketName) throws CloudException;

    boolean doesBucketExist(String bucketName) throws CloudException;

    InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws CloudException;

    UploadPartResult uploadPart(UploadPartRequest request) throws CloudException;

    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) throws CloudException;

    PutObjectResult putObject(String bucketName, String key, File file, ObjectMetadata metadata) throws CloudException;

    PutObjectResult putObject(String bucketName, String key, InputStream in, ObjectMetadata metadata)
            throws CloudException;

    default PutObjectResult putObject(String bucketName, String key, File file) {
        return putObject(bucketName, key, file, null);
    }

    CopyObjectResult copyObject(String bucketName, String from, String to)
            throws CloudException;

    DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) throws CloudException;

    String deleteObject(DeleteObjectRequest request) throws CloudException;

    boolean doesObjectExist(String bucketName, String key) throws CloudException;

    StorageObject getObject(String bucketName, String key) throws CloudException;

    ObjectMetadata getObject(GetObjectRequest request, File file) throws CloudException;

    ObjectMetadata getObjectMetadata(String bucketName, String key) throws CloudException;

    URL generatePresignedUrl(String bucketName, String key, Date expiration) throws CloudException;

    URL generatePresignedUrlWithCustomFileName(String bucketName, String key, Date expiration,
            String customFileName) throws CloudException;

    URL generatePresignedPutUrl(String bucketName, String key, Date expiration) throws CloudException;

    List<ObjectSummary> list(String bucketName, String prefix) throws CloudException;

}
