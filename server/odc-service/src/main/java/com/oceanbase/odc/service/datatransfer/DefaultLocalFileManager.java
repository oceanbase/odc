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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DefaultLocalFileManager}
 *
 * @author yh263208
 * @date 2022-07-28 14:49
 * @since ODC_release_3.4.0
 * @see LocalFileManager
 */
@Slf4j
public class DefaultLocalFileManager implements LocalFileManager {

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public File getWorkingDir(TaskType taskType, String bucket) throws IOException {
        String realSuffix;
        if (bucket == null) {
            realSuffix = "";
        } else {
            realSuffix = bucket.startsWith("/") ? bucket.substring(1) : bucket;
            if (LocalFileManager.UPLOAD_BUCKET.equals(bucket)) {
                /**
                 * <pre>
                 *     对于上传场景，非客户端模式下为了防止用户通过手动传 filename 的方式获取其他用户的文件，
                 *     这里针对上传场景做防护：使用用户 id 对文件进行隔离
                 * </pre>
                 */
                realSuffix = bucket + File.separator + authenticationFacade.currentUserId();
            }
        }
        return new File(getOrCreateFullPathAppendingSuffixToDataPath(taskType, realSuffix));
    }

    @Override
    public Optional<File> findByName(TaskType taskType, String bucket, @NonNull String fileName) throws IOException {
        File workingDir = getWorkingDir(taskType, bucket);
        for (File subFile : workingDir.listFiles()) {
            if (Objects.equals(fileName, subFile.getName())) {
                return Optional.of(subFile);
            }
        }
        return Optional.empty();
    }

    @Override
    public int copy(TaskType taskType, String bucket, @NonNull InputStream inputStream,
            @NonNull Supplier<String> fileNameSupplier) throws IOException {
        File workingDir = getWorkingDir(taskType, bucket);
        String fileName = fileNameSupplier.get();
        if (fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name, " + fileName);
        }
        File target = new File(workingDir.getAbsoluteFile() + File.separator + fileName);
        PreConditions.validNoPathTraversal(target, workingDir.getAbsolutePath());
        try (FileOutputStream outputStream = new FileOutputStream(target)) {
            return IOUtils.copy(inputStream, outputStream);
        }
    }

    @Override
    public String getUploadBucket(String bucket) {
        /**
         * 非客户端模式下，下载上传文件需要到上传目录中。由于上传的时候根据用户的 id 做了文件隔离， 触发下载操作的可能是文件的 上传者（也就是任务的发起者），也可能是流程的审核者，因此这里需要在
         * bucket 中拼入任务的创建者 id，来正确找到文件。
         */
        return LocalFileManager.UPLOAD_BUCKET + File.separator + bucket;
    }

    private String getOrCreateFullPathAppendingSuffixToDataPath(TaskType taskType, @NonNull String suffix)
            throws IOException {
        String dataDir = MoreObjects.firstNonNull(SystemUtils.getEnvOrProperty("file.storage.dir"), "./data");
        String taskDir;
        switch (taskType) {
            case EXPORT:
            case IMPORT:
                taskDir = "data_transfer";
                break;
            case EXPORT_RESULT_SET:
                taskDir = FileBucket.RESULT_SET.name();
                break;
            default:
                taskDir = taskType.name();
        }
        File file = Paths.get(dataDir, taskDir, suffix).toFile();
        if (!file.exists()) {
            FileUtils.forceMkdir(file);
        }
        return file.getAbsolutePath();
    }

}
