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

@Data
public class GenericRequest {
    private String bucketName;
    private String key;
    private String versionId;

    public GenericRequest() {}

    public GenericRequest(String bucketName) {
        this(bucketName, null);
    }

    public GenericRequest(String bucketName, String key) {
        this.bucketName = bucketName;
        this.key = key;
    }

    public GenericRequest(String bucketName, String key, String versionId) {
        this.bucketName = bucketName;
        this.key = key;
        this.versionId = versionId;
    }

}
