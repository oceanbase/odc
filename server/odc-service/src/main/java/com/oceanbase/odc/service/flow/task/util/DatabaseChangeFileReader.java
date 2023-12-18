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
package com.oceanbase.odc.service.flow.task.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/18
 * @since ODC_release_4.2.0
 */
@Slf4j
@Component
public class DatabaseChangeFileReader {

    @Autowired
    private ObjectStorageFacade storageFacade;

    public InputStream readInputStreamFromSqlObjects(DatabaseChangeParameters params, String bucketName,
            long maxSizeBytes) {
        List<String> objectIds = params.getSqlObjectIds();
        if (CollectionUtils.isEmpty(objectIds)) {
            return null;
        }
        try {
            return readSqlFilesStream(bucketName, objectIds, maxSizeBytes);
        } catch (Exception e) {
            log.warn("Failed to read sql files from object storage", e);
            throw new IllegalStateException("Failed to read sql files from object storage");
        }
    }

    private InputStream readSqlFilesStream(String bucket, List<String> objectIds, long maxBytes) throws IOException {
        long totalBytes = 0;
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        for (String objectId : objectIds) {
            StorageObject object = storageFacade.loadObject(bucket, objectId);
            InputStream current = object.getContent();
            totalBytes += object.getMetadata().getTotalLength();
            // remove UTF-8 BOM if exists
            current.mark(3);
            byte[] byteSql = new byte[3];
            if (current.read(byteSql) >= 3 && byteSql[0] == (byte) 0xef && byteSql[1] == (byte) 0xbb
                    && byteSql[2] == (byte) 0xbf) {
                current.reset();
                current.skip(3);
            } else {
                current.reset();
            }
            if (maxBytes > 0 && totalBytes > maxBytes) {
                log.info("The file size is too large and will not be read later, totalSize={} bytes", totalBytes);
                return inputStream;
            }
            inputStream = new SequenceInputStream(inputStream, current);
        }
        return inputStream;
    }

}
