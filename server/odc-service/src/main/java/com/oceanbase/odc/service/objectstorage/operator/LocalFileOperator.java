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
package com.oceanbase.odc.service.objectstorage.operator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.objectstorage.BufferedIterableInputStream;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.util.ObjectStorageUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/4 下午5:27
 * @Description: [Responsible for operating local files, like read/write local file, etc.]
 */
@Slf4j
@Component
public class LocalFileOperator {
    /**
     * 限制单次写入数据大小为 1M.
     */
    private static final int BUFFER_SIZE = 1_048_576;

    private final String fileSeparator = File.separator;

    @Getter
    private final String localDir;

    private Path fileDirPath;

    public LocalFileOperator(
            @Value("${odc.objectstorage.local.dir:#{systemProperties['user.home'].concat(T(java.io.File).separator).concat('data').concat"
                    + "(T(java.io.File).separator).concat('files')}}") String localDir) {
        this.localDir = localDir;
    }

    @PostConstruct
    public void init() {
        fileDirPath = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(fileDirPath);
            log.info("LocalFileOperator init, fileDirPath={}", fileDirPath.toString());
        } catch (Exception ex) {
            throw new InternalServerError(ex.getMessage());
        }
    }

    /**
     * 保存到本地文件
     *
     * @param bucket 存储空间名
     * @param objectName 存储对象名
     * @param totalLength 文件大小，单位为字节
     * @param inputStream 存储的流对象
     * @return 文件 sha1
     */
    public String saveLocalFile(String bucket, String objectName, long totalLength, InputStream inputStream) {
        Verify.notEmpty(bucket, "bucket");
        Verify.notEmpty(objectName, "objectName");
        Verify.notNull(inputStream, "inputStream");
        PreConditions.validNoPathTraversal(generateFilePath(bucket, objectName), generateFilePath(bucket));
        File localFile = getOrCreateLocalFile(bucket, objectName);
        try {
            return saveToLocalByBlock(localFile, totalLength, inputStream);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("Write content to local fail, ex={}", e);
            throw new UnexpectedException("Write content to local failed");
        }
    }

    /**
     * 获取或者创建本地文件
     *
     * @param bucketName 存储空间名
     * @param objectId 存储对象 ID
     */
    public File getOrCreateLocalFile(String bucketName, String objectId) {
        Verify.notEmpty(bucketName, "bucketName");
        Verify.notEmpty(objectId, "objectId");
        PreConditions.validNoPathTraversal(generateFilePath(bucketName, objectId), generateFilePath(bucketName));
        File file = new File(absolutePathName(bucketName, objectId));
        createParentDirs(file);
        return file;
    }

    /**
     * 删除本地文件
     *
     * @param bucketName 存储空间名
     * @param objectId 存储对象 ID
     */
    public boolean deleteLocalFile(String bucketName, String objectId) {
        Verify.notEmpty(bucketName, "bucketName");
        Verify.notEmpty(objectId, "objectId");
        String absolutePath = absolutePathName(bucketName, objectId);
        PreConditions.validNoPathTraversal(generateFilePath(bucketName, objectId), generateFilePath(bucketName));
        File localFile = new File(absolutePath);
        return deleteFileQuietly(localFile);
    }

    /**
     * 根据文件元信息判断 localhost 是否存在与元信息对应的文件. <br>
     * 1. 判断本地文件是否存在 <br>
     * 2. 判断本地文件大小是否与文件元信息相等 <br>
     * 3. 判断文件 sha1 是否于元信息的 sha1 相同
     *
     * @param objectMetadata 存储对象元信息
     * @return 本地文件是否存在
     */
    public boolean isLocalFileAbsent(ObjectMetadata objectMetadata) {
        String bucketName = objectMetadata.getBucketName();
        String objectId = objectMetadata.getObjectId();
        File file = getOrCreateLocalFile(bucketName,
                ObjectStorageUtils.concatObjectId(objectId, objectMetadata.getExtension()));
        if (!file.exists()) {
            log.warn("Local File not exists, bucketName={}, objectId={}", bucketName, objectId);
            return true;
        }
        long fileSize = file.length();
        if (fileSize != objectMetadata.getTotalLength()) {
            log.warn("File size not match, objectId={}", objectMetadata.getObjectId());
            return true;
        }
        String sha1;
        try {
            sha1 = HashUtils.sha1(file);
        } catch (IOException e) {
            throw new UnexpectedException("get file sha1 failed");
        }
        if (!StringUtils.equalsIgnoreCase(sha1, objectMetadata.getSha1())) {
            log.warn("sha1 not match, should refresh from database, fileName={}", objectMetadata.getObjectName());
            return true;
        }
        return false;
    }

    /**
     * 加载本地文件.
     */
    public Resource loadAsResource(String bucketName, String objectId) {
        PreConditions.validNoPathTraversal(generateFilePath(bucketName, objectId), generateFilePath(bucketName));
        try {
            Resource resource =
                    new UrlResource(fileDirPath.resolve(absolutePathName(bucketName, objectId)).normalize().toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new NotFoundException(ResourceType.ODC_FILE, "fileName", resource.getFilename());
            }
        } catch (MalformedURLException e) {
            throw new UnexpectedException("A malformed URL has occurred");
        }
    }


    private String saveToLocalByBlock(File file, long totalLength, InputStream inputStream)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedIterableInputStream iteratorStream =
                        new BufferedIterableInputStream(inputStream, BUFFER_SIZE, totalLength)) {
            while (iteratorStream.hasNext()) {
                byte[] block = iteratorStream.next();
                digest.update(block);
                fileOutputStream.write(block);
            }
        }
        // 避免转换成 16 进制时丢失高位的 0，丢失高位 0 是会跟 linux 下的 sha1sum 计算出的结果不一致.
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void createParentDirs(File file) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
    }

    private String absolutePathName(String bucket, String fileName) {
        return localDir + fileSeparator + bucket + fileSeparator + fileName;
    }

    private boolean deleteFileQuietly(File localFile) {
        boolean ret = FileUtils.deleteQuietly(localFile);
        if (ret) {
            log.info("delete local file, absolutePathName={}", localFile.getAbsolutePath());
        }
        return ret;
    }

    private String generateFilePath(String bucket) {
        return localDir.concat(fileSeparator).concat(bucket);
    }

    private String generateFilePath(String bucket, String objectName) {
        return localDir.concat(fileSeparator).concat(bucket).concat(fileSeparator).concat(objectName);
    }



}
