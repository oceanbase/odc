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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.objectstorage.ObjectBlockEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectBlockRepository;
import com.oceanbase.odc.service.objectstorage.BufferedIterableInputStream;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/4 下午5:28
 * @Description: [Responsible for operating object blocks in the database, like save/load local file
 *               in db by block, etc.]
 */
@Slf4j
@Component
public class ObjectBlockOperator {
    /**
     * 单个分块上传超时时间，单位是秒，默认值为 60 秒
     */
    @Value("${odc.objectstorage.upload-timeout-seconds:60}")
    private long UPLOAD_TIMEOUT_SECONDS = 60L;

    @Autowired
    private ObjectBlockRepository blockRepository;

    /**
     * 分块保存本地文件数据到数据库
     *
     * @param meta 对象元信息
     * @param localFile 本地文件
     */
    public void saveObjectBlock(ObjectMetadata meta, File localFile) {
        Verify.verify(localFile.exists() && localFile.isFile(), "File not exist");
        try {
            deleteByObjectId(meta.getObjectId());
            saveLocalFileToDbByBlock(meta, localFile);
        } catch (IOException e) {
            log.warn("Write content to db fail.", e);
            throw new UnexpectedException("save file to db by block failed");
        }
    }

    /**
     * 根据对象 id 获取文件块的迭代器.
     *
     * @param objectId 对象唯一 id
     * @return 块内容
     */
    public ObjectBlockIterator getBlockIterator(String objectId) {
        return new ObjectBlockIterator(blockRepository, objectId);
    }

    /**
     * 使用存储对象 id 删除数据库中的文件
     *
     * @param objectId 文件ID
     * @return deleted block count
     */
    public int deleteByObjectId(String objectId) {
        int deletedBlockCount = blockRepository.deleteByObjectId(objectId);
        log.info("deleteByFileId, deletedBlockCount={}", deletedBlockCount);
        return deletedBlockCount;
    }

    /**
     * 根据文件id批量删除文件块信息.
     *
     * @param objectIds 文件id集合
     * @return 删除记录数量
     */
    public int batchDelete(Collection<String> objectIds) {
        if (CollectionUtils.isEmpty(objectIds)) {
            return 0;
        }
        return blockRepository.deleteAllByObjectIdsIn(objectIds);
    }

    /**
     * 根据文件的文件块最近更新时间判断文件块是否上传失败
     *
     * @param objectId 文件 id
     * @return 文件是否上传失败
     */
    public boolean isUploadingFailed(String objectId) {
        Date date = blockRepository
                .findMaxUpdateTimeByObjectId(objectId).orElse(new Date());
        return TimeUtils.absMillisBetween(date.toInstant(), new Date().toInstant()) > TimeUnit.SECONDS
                .toMillis(UPLOAD_TIMEOUT_SECONDS);
    }

    private void saveLocalFileToDbByBlock(ObjectMetadata meta, File localFile) throws IOException {
        long totalSize = meta.getTotalLength();
        int blockSize = (int) meta.getSplitLength();
        long index = 0L;
        String objectId = meta.getObjectId();
        try (FileInputStream fileInputStream = new FileInputStream(localFile);
                BufferedIterableInputStream iteratorStream =
                        new BufferedIterableInputStream(fileInputStream, blockSize, totalSize);) {
            while (iteratorStream.hasNext()) {
                // 如果出现单块 block 超过限制时间还没有保存成功，那么停止保存
                if (isUploadingFailed(objectId)) {
                    throw new InternalServerError(ErrorCodes.FileWriteFailed,
                            "save object to db failed due to timeout");
                }
                byte[] block = iteratorStream.next();
                saveToDb(objectId, index++, block);
            }
        }
    }

    private void saveToDb(String objectId, long index, byte[] content) {
        ObjectBlockEntity entity = new ObjectBlockEntity();
        entity.setObjectId(objectId);
        entity.setIndex(index);
        entity.setLength(content.length);
        entity.setContent(content);
        blockRepository.saveAndFlush(entity);
    }

}
