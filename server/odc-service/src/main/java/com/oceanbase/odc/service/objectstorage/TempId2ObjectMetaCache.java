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
package com.oceanbase.odc.service.objectstorage;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/24 下午7:48
 * @Description: [This class is a cache storing mapping from a temp id to its corresponding object
 *               metadata.]
 */
@Slf4j
@Component
public class TempId2ObjectMetaCache {
    /**
     * entry 写入一分钟后，自动失效
     */
    Cache<String, ObjectMetadata> tempId2ObjectMeta =
            Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    /**
     * return {@code null} if key is {@code null} or if there is no cached value for the key
     */
    public ObjectMetadata get(String tempId) {
        if (StringUtils.isNotEmpty(tempId)) {
            return tempId2ObjectMeta.getIfPresent(tempId);
        }
        return null;
    }

    /**
     * put entry into cache, return null if either key or value is null
     */
    public ObjectMetadata put(String tempId, ObjectMetadata objectMetadata) {
        if (StringUtils.isNotEmpty(tempId) && Objects.nonNull(objectMetadata)) {
            tempId2ObjectMeta.put(tempId, objectMetadata);
            return objectMetadata;
        }
        return null;
    }

    /**
     * discards any cached value for the key quietly
     */
    public void remove(String tempId) {
        if (StringUtils.isNotEmpty(tempId)) {
            tempId2ObjectMeta.invalidate(tempId);
        }
    }
}
