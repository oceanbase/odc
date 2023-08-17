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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import lombok.Data;

@Data
public abstract class StorageObject extends GenericResult implements Closeable {
    private String key;
    private String bucketName;
    private ObjectMetadata metadata = new ObjectMetadata();

    public abstract InputStream getObjectContent();

    public abstract InputStream getAbortableContent();

    protected void setObjectContent(InputStream stream) {
        throw new RuntimeException("setObjectContent method not supported");
    }

    @Override
    public void close() throws IOException {
        InputStream objectContent = getObjectContent();
        if (objectContent != null) {
            objectContent.close();
        }
    }
}
