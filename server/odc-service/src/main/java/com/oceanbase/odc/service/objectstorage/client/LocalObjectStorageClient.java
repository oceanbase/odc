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
package com.oceanbase.odc.service.objectstorage.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockIterator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockOperator;

/**
 * @author keyang
 * @date 2024/08/09
 * @since 4.3.2
 */
public class LocalObjectStorageClient implements ObjectStorageClient {

    private ObjectBlockOperator blockOperator;
    private long blockSplitLength;

    public LocalObjectStorageClient(ObjectBlockOperator blockOperator, long blockSplitLength) {
        this.blockOperator = blockOperator;
        this.blockSplitLength = blockSplitLength;
    }

    @Override
    public URL generateDownloadUrl(String objectName, Long expirationSeconds, String customFileName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URL generateUploadUrl(String objectName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putObject(String objectName, File file, ObjectTagging objectTagging) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectId(objectName);
        metadata.setSplitLength(blockSplitLength);
        metadata.setTotalLength(file.length());
        blockOperator.saveObjectBlock(metadata, file);
    }

    @Override
    public byte[] readContent(String objectName) throws IOException {
        ObjectBlockIterator blockIterator = blockOperator.getBlockIterator(objectName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (blockIterator.hasNext()) {
            outputStream.write(blockIterator.next());
        }
        return outputStream.toByteArray();
    }

    @Override
    public void downloadToFile(String objectName, File targetFile) throws IOException {
        ObjectBlockIterator iterator = blockOperator.getBlockIterator(objectName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
            while (iterator.hasNext()) {
                fileOutputStream.write(iterator.next());
            }
        }
    }

    @Override
    public List<String> deleteObjects(List<String> objectNames) throws IOException {
        if (CollectionUtils.isEmpty(objectNames)) {
            return new ArrayList<>();
        }
        HashSet<String> objectNameSet = new HashSet<>(objectNames);
        blockOperator.batchDelete(objectNameSet);
        return new ArrayList<>();
    }

    @Override
    public String deleteObject(@NotBlank String objectName) throws IOException {
        blockOperator.deleteByObjectId(objectName);
        return objectName;
    }

    @Override
    public InputStream getObject(String objectName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream getAbortableObject(String objectName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
