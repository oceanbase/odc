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

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.datatransfer.file.DefaultLocalFileManager;
import com.oceanbase.odc.service.datatransfer.file.LocalFileManager;

import lombok.NonNull;

/**
 * {@link DesktopLocalFileManager}
 *
 * @author yh263208
 * @date 2022-07-28 16:56
 * @since ODC_release_3.4.0
 */
@Profile("clientMode")
@Service
@SkipAuthorize("odc internal usage")
public class DesktopLocalFileManager extends DefaultLocalFileManager {

    @Override
    public Optional<File> findByName(TaskType taskType, String bucket, @NonNull String fileName) throws IOException {
        if (LocalFileManager.UPLOAD_BUCKET.equals(bucket)) {
            File target = new File(fileName);
            return target.exists() ? Optional.of(target) : Optional.empty();
        }
        return super.findByName(taskType, bucket, fileName);
    }

    @Override
    public int copy(TaskType taskType, String bucket, @NonNull InputStream inputStream,
            @NonNull Supplier<String> fileNameSupplier) throws IOException {
        if (LocalFileManager.UPLOAD_BUCKET.equals(bucket)) {
            throw new UnsupportedOperationException("Upload is not allowed when client mode");
        }
        return super.copy(taskType, bucket, inputStream, fileNameSupplier);
    }

    @Override
    public String getUploadBucket(String bucket) {
        return LocalFileManager.UPLOAD_BUCKET;
    }

}
