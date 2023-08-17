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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.oceanbase.odc.service.objectstorage.model.Bucket;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;

public interface ObjectStorageFacade {
    ObjectMetadata putObject(String bucket, String objectName, long totalLength,
            InputStream inputStream);

    ObjectMetadata putTempObject(String bucket, String objectName, long totalLength, InputStream inputStream);

    ObjectMetadata putTempObject(String bucket, String objectName, long userId, long totalLength,
            InputStream inputStream);

    ObjectMetadata putObject(String bucket, String objectName, long userId, long totalLength,
            InputStream inputStream);

    /**
     * Obtain object download url by bucket and objectId
     */
    String getDownloadUrl(String bucket, String objectId);

    /**
     * Update object in a specific bucket by objectId
     */
    ObjectMetadata updateObject(String bucket, String objectName, String objectId, long totalLength,
            InputStream inputStream);

    /**
     * Delete object by bucket and objectId
     */
    ObjectMetadata deleteObject(String bucket, String objectId);

    /**
     * Load object by bucket name and objectId
     */
    StorageObject loadObject(String bucket, String objectId) throws IOException;

    /**
     * Load object as string by bucket name and objectId
     */
    String loadObjectContentAsString(String bucket, String objectId) throws IOException;

    ObjectMetadata loadMetaData(String bucket, String objectId);

    /**
     * Load object under a bucket
     */
    List<StorageObject> loadObject(String bucket) throws IOException;

    /**
     * Predicate if an object exists
     */
    boolean isObjectExists(String bucket, String objectId);

    /**
     * Create a bucket by name
     */
    Bucket createBucketIfNotExists(String name);

    /**
     * Get bucket by name
     */
    Bucket getBucket(String name);

    /**
     * Predicate if a bucket exists
     */
    boolean isBucketExists(String name);
}
