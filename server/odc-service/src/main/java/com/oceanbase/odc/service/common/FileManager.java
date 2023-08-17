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
package com.oceanbase.odc.service.common;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.WebResponseUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/3/21
 */

@Slf4j
@Service
@SkipAuthorize("file path traversal check inside")
public class FileManager {
    private static final String fileSeparator = File.separator;
    public static final String basePath = "data";
    private final ScheduledExecutorService scheduleExecutor;
    public final Map<FileBucket, String> pathMap;

    @Autowired
    public FileManager(@Value("${file.storage.dir:./data}") String baseFileStorageDir) {
        String asyncFileStorageDir = generatePath(false, baseFileStorageDir, FileBucket.ASYNC.name());
        pathMap = new HashMap<>();
        pathMap.put(FileBucket.ASYNC, asyncFileStorageDir);
        createFileDir();

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("odc-file-manager-schedule-%d")
                .build();
        scheduleExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduleExecutor.scheduleAtFixedRate(new FileRecycler(asyncFileStorageDir), 5, 10, TimeUnit.MINUTES);
        log.info("odc file manager initialized");
    }

    @PreDestroy
    public void destroy() {
        log.info("file manager destroy...");
        ExecutorUtils.gracefulShutdown(scheduleExecutor, "fileManagerScheduleExecutor", 5);
        log.info("file manager destroyed");
    }

    private void createFileDir() {
        for (String path : pathMap.values()) {
            File fileDir = new File(path);
            if (!fileDir.exists()) {
                boolean success = fileDir.mkdirs();
                if (!success) {
                    log.error("Create file directory failed, fileDir={} create_status={}", fileDir, success);
                    throw new InternalServerError(ErrorCodes.FileCreateUnauthorized,
                            "Failed to create file dir: " + path);
                }
            }
        }
    }

    public String generatePath(boolean includeFileName, String... parts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : parts) {
            stringBuilder.append(part).append(fileSeparator);
        }
        if (includeFileName) {
            // delete the final file separator
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public Object download(FileBucket fileBucket, String id) throws IOException {
        String filePath = generatePath(fileBucket, id);
        String fileNameForDownload = id;
        if (fileBucket.equals(FileBucket.ASYNC)) {
            throw new UnsupportedOperationException("Not supported for async task");
        }
        String expectedPath = generatePath(fileBucket);
        PreConditions.validNoPathTraversal(filePath, expectedPath);

        FileSystemResource fileResource = new FileSystemResource(filePath);

        PreConditions.validExists(fileResource.getFile());

        return WebResponseUtils.getFileAttachmentResponseEntity(new InputStreamResource(fileResource.getInputStream()),
                fileNameForDownload);
    }

    public static String generatePath(FileBucket bucket, String id) {
        generateDir(bucket);
        return String.format("%s/%s/%s", FileManager.basePath, bucket.name(), id);
    }

    public static String generatePath(FileBucket bucket) {
        generateDir(bucket);
        return String.format("%s/%s", FileManager.basePath, bucket.name());
    }

    public static String generateDir(FileBucket bucket) {
        String dirPath = String.format("%s/%s", FileManager.basePath, bucket.name());
        try {
            FileUtils.forceMkdir(new File(dirPath));
        } catch (IOException ex) {
            throw new RuntimeException("create directory failed, dirPath=" + dirPath);
        }
        return dirPath;
    }

    public static String generateDirPath(FileBucket bucket, String id) {
        String dirPath = String.format("%s/%s/%s", FileManager.basePath, bucket.name(), id);
        try {
            FileUtils.forceMkdir(new File(dirPath));
        } catch (IOException ex) {
            throw new RuntimeException("create directory failed, dirPath=" + dirPath);
        }
        return dirPath;
    }

    public static String generateBaseDownloadUrl(FileBucket bucket) {
        PreConditions.notNull(bucket, "bucket");
        return String.format("/api/v2/objectstorage/%s/files/", bucket.name());
    }
}
