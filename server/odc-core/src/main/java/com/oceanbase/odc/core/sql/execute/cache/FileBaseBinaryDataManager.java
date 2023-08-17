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
package com.oceanbase.odc.core.sql.execute.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;

import lombok.NonNull;

/**
 * File-based binary data manager, mainly using file random reading technology to serialize binary
 * data to disk and provide random read strategy
 *
 * @author yh263208
 * @date 2021-11-03 17:33
 * @since ODC_release_3.2.2
 * @see BinaryDataManager
 */
public class FileBaseBinaryDataManager implements BinaryDataManager {
    /**
     * Working directory, the file manager will manage files in this directory
     */
    private final File workingDir;
    private final PageManager pageManager;
    /**
     * 512 MB
     */
    private static final int MAX_SINGLE_STORAGE_FILE_SIZE = 1024 * 1024 * 512;
    private PagedRandomAccessFile currentWriteFile;

    public FileBaseBinaryDataManager(@NonNull String workingDir) throws IOException {
        this.workingDir = new File(workingDir);
        if (!this.workingDir.exists()) {
            throw new FileNotFoundException("Input path does not exist, workingDir=" + workingDir);
        }
        if (!this.workingDir.isDirectory()) {
            throw new IllegalArgumentException("Input string is not a directory, workingDir=" + workingDir);
        }
        this.pageManager = new PageManager(workingDir);
    }

    @Override
    public synchronized BinaryContentMetaData write(@NonNull InputStream inputStream) throws IOException {
        reloadCurrentFile();
        int offset = currentWriteFile.length();
        int fileSize = inputStream.available();
        try {
            currentWriteFile.seekForWrite(offset);
            byte[] buffer = new byte[1024 * 4];
            int length = inputStream.read(buffer);
            while (length != -1) {
                currentWriteFile.write(buffer, 0, length);
                length = inputStream.read(buffer, 0, length);
            }
            inputStream.close();
            return new BinaryContentMetaData(this.currentWriteFile.getPath(), offset, fileSize);
        } finally {
            currentWriteFile.remove();
        }
    }

    @Override
    public InputStream read(@NonNull BinaryContentMetaData metaData) throws IOException {
        PagedRandomAccessFile pagedRandomAccessFile = currentWriteFile;
        if (!Objects.equals(metaData.getFilePath(), currentWriteFile.getPath())) {
            pagedRandomAccessFile = new PagedRandomAccessFile(metaData.getFilePath(), pageManager);
        }
        try {
            pagedRandomAccessFile.seekForRead((int) metaData.getOffset());
            int bufferSize = 10 * 1024;
            int loopCount = metaData.getSizeInBytes() / bufferSize;
            if (loopCount == 0) {
                byte[] buffer = new byte[metaData.getSizeInBytes()];
                pagedRandomAccessFile.read(buffer);
                return new ByteArrayInputStream(buffer);
            }
            byte[] buffer = new byte[bufferSize];
            int length = pagedRandomAccessFile.read(buffer);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(metaData.getSizeInBytes());
            while ((--loopCount) > 0 && length != -1) {
                outputStream.write(buffer, 0, length);
                length = pagedRandomAccessFile.read(buffer, 0, length);
            }
            outputStream.write(buffer, 0, length);
            int modValue = metaData.getSizeInBytes() % bufferSize;
            if (modValue != 0) {
                buffer = new byte[modValue];
                pagedRandomAccessFile.read(buffer);
                outputStream.write(buffer);
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        } finally {
            pagedRandomAccessFile.remove();
        }
    }

    @Override
    public void close() throws Exception {
        if (this.currentWriteFile != null) {
            this.currentWriteFile.close();
        }
        this.pageManager.close();
        FileUtils.forceDelete(this.workingDir);
    }

    @Override
    public String toString() {
        return "FileBaseBinaryDataManager: " + this.workingDir.getAbsolutePath();
    }

    private PagedRandomAccessFile createStorageFile() throws IOException {
        File destFile = new File(this.workingDir.getAbsolutePath() + "/" + generateFileName());
        if (destFile.exists()) {
            throw new IllegalStateException("Unknown error...");
        }
        if (!destFile.createNewFile()) {
            throw new IOException("Failed to create a file, fileName=" + destFile.getAbsolutePath());
        }
        return new PagedRandomAccessFile(destFile.getAbsolutePath(), pageManager);
    }

    private void reloadCurrentFile() throws IOException {
        if (this.currentWriteFile == null) {
            this.currentWriteFile = createStorageFile();
            return;
        }
        if (this.currentWriteFile.length() >= MAX_SINGLE_STORAGE_FILE_SIZE) {
            this.currentWriteFile.close();
            this.currentWriteFile = createStorageFile();
        }
    }

    private String generateFileName() {
        return "FileBaseBinaryDataManager_".toLowerCase() + UUID.randomUUID().toString().replaceAll("-", "") + ".meta";
    }

}
