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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.CloseableIterator;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.common.util.SqlUtils;
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

    public List<OffsetString> loadSqlContentFromUserInput(DatabaseChangeParameters params, DialectType dialectType) {
        List<OffsetString> ret = new ArrayList<>();
        try {
            String sqlContent = params.getSqlContent();
            if (StringUtils.isNotBlank(sqlContent)) {
                ret.addAll(SqlUtils.splitWithOffset(dialectType, sqlContent, params.getDelimiter()));
            }
            return ret;
        } catch (Exception e) {
            log.warn("Failed to read sql content from user input", e);
            throw new IllegalStateException("Failed to read sql content from user input");
        }
    }

    public CloseableIterator<String> loadSqlIteratorFromFiles(DatabaseChangeParameters params, DialectType dialectType,
            String bucketName, long maxSizeBytes) {
        List<String> objectIds = params.getSqlObjectIds();
        if (CollectionUtils.isEmpty(objectIds)) {
            return null;
        }
        try {
            InputStream inputStream = readSqlFilesStream(bucketName, objectIds, maxSizeBytes);
            return SqlUtils.iterator(dialectType, params.getDelimiter(), inputStream, StandardCharsets.UTF_8);
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
