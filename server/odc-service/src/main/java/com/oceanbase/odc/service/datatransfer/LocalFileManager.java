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
package com.oceanbase.odc.service.datatransfer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.NonNull;

/**
 * {@link LocalFileManager}
 *
 * @author yh263208
 * @date 2022-07-28 14:08
 * @since ODC_release_3.4.0
 */
public interface LocalFileManager {
    String UPLOAD_BUCKET = "upload";

    /**
     * get working dir
     *
     * @param bucket This parameter can be any value that can mark the task, eg. taskId. {@code null} is
     *        acceptable
     * @return working dir
     */
    File getWorkingDir(TaskType taskType, String bucket) throws IOException;

    /**
     * find file by name
     *
     * @param bucket target bucket
     * @param fileName name of the file
     * @return file
     */
    Optional<File> findByName(TaskType taskType, String bucket, @NonNull String fileName) throws IOException;

    /**
     * write {@link InputStream} to dest bucket
     *
     * @param bucket bucket name
     * @param inputStream {@link InputStream}
     * @param fileNameSupplier name {@link Supplier}
     * @return write data size in byte
     */
    int copy(TaskType taskType, String bucket, @NonNull InputStream inputStream,
            @NonNull Supplier<String> fileNameSupplier) throws IOException;

    String getUploadBucket(String bucket);

}
