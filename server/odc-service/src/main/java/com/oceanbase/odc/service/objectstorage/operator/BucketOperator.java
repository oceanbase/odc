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

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.objectstorage.BucketEntity;
import com.oceanbase.odc.metadb.objectstorage.BucketRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.model.Bucket;
import com.oceanbase.odc.service.objectstorage.util.BucketMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/10 下午8:26
 * @Description: []
 */
@Slf4j
@Component
public class BucketOperator {
    /**
     * bucket 格式如下.
     * <p>
     * 只能包括小写字母、数字、短划线（-）和 斜杠（/）<br>
     * 必须以小写字母或者数字开头和结尾。<br>
     * 长度必须在3~63字节之间。<br>
     */
    private static final Pattern BUCKET_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-\\\\/]{1,61}[a-z0-9]$");

    private BucketMapper mapper = BucketMapper.INSTANCE;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    /**
     * 创建存储空间
     *
     * @param bucketName 存储空间名
     */
    public Bucket createBucket(String bucketName) {
        validateName(bucketName);
        PreConditions.validNoDuplicated(ResourceType.ODC_BUCKET, "bucketName", bucketName,
                () -> isBucketExist(bucketName));
        BucketEntity bucket = new BucketEntity();
        bucket.setName(bucketName);
        bucket.setCreatorId(authenticationFacade.currentUserId());
        return mapper.entityToModel(bucketRepository.saveAndFlush(bucket));
    }

    /**
     * 根据存储空间名获取存储空间
     *
     * @param name 存储空间名
     */
    public Optional<Bucket> getBucketByName(String name) {
        validateName(name);
        Optional<BucketEntity> optional = bucketRepository.findByName(name);
        return optional.map(mapper::entityToModel);
    }

    /**
     * 根据存储空间名删除存储空间
     *
     * @param name 存储空间名
     */
    public void deleteBucketByName(String name) {
        validateName(name);
        bucketRepository.deleteByName(name);
    }

    /**
     * 分页获取存储空间
     *
     * @param pageable 分页
     */
    public Page<Bucket> listBucket(Pageable pageable) {
        Page<BucketEntity> entityPage = bucketRepository.findAll(pageable);
        return entityPage.map(mapper::entityToModel);
    }

    /**
     * 根据存储空间名判断存储空间是否存在
     *
     * @param name 存储空间名
     */
    public boolean isBucketExist(String name) {
        validateName(name);
        return bucketRepository.findByName(name).isPresent();
    }


    private void validateName(String name) {
        PreConditions.validArgumentState(BUCKET_NAME_PATTERN.matcher(name).matches(), ErrorCodes.BadArgument, null,
                "bucket name is invalid");
    }
}
