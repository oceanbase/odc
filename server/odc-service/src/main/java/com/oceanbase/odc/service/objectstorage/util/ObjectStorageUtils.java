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
package com.oceanbase.odc.service.objectstorage.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.flow.task.model.SizeAwareInputStream;
import com.oceanbase.odc.service.objectstorage.ObjectStorageHandler;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/29 下午9:55
 * @Description: []
 */
@Slf4j
public class ObjectStorageUtils {

    public static String concatObjectId(String objectId, String extension) {
        Verify.notEmpty(objectId, "objectId");
        if (StringUtils.isNotEmpty(extension)) {
            return objectId.concat(".").concat(extension);
        }
        return objectId;
    }

    public static SizeAwareInputStream loadObjectsForTask(@NonNull List<ObjectMetadata> metadatas,
            CloudObjectStorageService cloudOSS, String executorDataPath, long maxReadBytes) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        long totalBytes = 0;
        for (ObjectMetadata metadata : metadatas) {
            StorageObject object = new ObjectStorageHandler(cloudOSS, executorDataPath).loadObject(metadata);
            InputStream current = object.getContent();
            totalBytes += object.getMetadata().getTotalLength();
            // remove UTF-8 BOM if exists
            current.mark(3);
            byte[] byteSql = new byte[3];
            if (current.read(byteSql) >= 3 && byteSql[0] == (byte) 0xef && byteSql[1] == (byte) 0xbb
                    && byteSql[2] == (byte) 0xbf) {
                current.reset();
                current.skip(3);
                totalBytes -= 3;
            } else {
                current.reset();
            }
            if (maxReadBytes > 0 && totalBytes > maxReadBytes) {
                log.info("The file size is too large and will not be read later, totalSize={} bytes", totalBytes);
                break;
            }
            inputStream = new SequenceInputStream(inputStream, current);
        }
        SizeAwareInputStream ret = new SizeAwareInputStream();
        ret.setInputStream(inputStream);
        ret.setTotalBytes(totalBytes);
        return ret;
    }

}
