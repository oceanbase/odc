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
package com.oceanbase.odc.service.objectstorage.cloud.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/10/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeleteObjectRequest extends GenericRequest {
    public DeleteObjectRequest() {}

    public DeleteObjectRequest(String bucketName) {
        super(bucketName);
    }

    public DeleteObjectRequest(String bucketName, String key) {
        super(bucketName, key);
    }

    public DeleteObjectRequest(String bucketName, String key, String versionId) {
        super(bucketName, key, versionId);
    }
}
