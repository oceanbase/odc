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
package com.oceanbase.odc.service.objectstorage.cloud.client;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
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
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadObjectTemporaryCredential;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;

public class NullCloudClient implements CloudClient {
    @Override
    public boolean supported() {
        return false;
    }

    @Override
    public String getBucketLocation(String bucketName) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public boolean doesBucketExist(String bucketName) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request)
            throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
            throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file, ObjectMetadata metadata)
            throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, InputStream in, ObjectMetadata metadata)
            throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public CopyObjectResult copyObject(String bucketName, String from, String to)
            throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public String deleteObject(DeleteObjectRequest request) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public boolean doesObjectExist(String bucketName, String key) throws CloudException {
        return false;
    }

    @Override
    public StorageObject getObject(String bucketName, String key) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest request, File file) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public URL generatePresignedUrlWithCustomFileName(String bucketName, String key, Date expiration,
            String customFileName) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public URL generatePresignedPutUrl(String bucketName, String key, Date expiration) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public List<ObjectSummary> list(String bucketName, String prefix) throws CloudException {
        throw new UnsupportedException();
    }

    @Override
    public UploadObjectTemporaryCredential generateTempCredential(String bucketName, String fileName,
            Long durationSeconds)
            throws CloudException {
        throw new UnsupportedException();
    }
}
