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
package com.oceanbase.odc.server.web.highavailable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.oceanbase.odc.server.web.highavailable.RateLimitProperties.BucketProperties;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitBucketProvider {

    @Autowired
    private RateLimitProperties rateLimitProperties;

    private final Map<String, BucketHolder> userId2ApiBucket = new ConcurrentHashMap<>();
    private final Map<String, BucketHolder> userId2RowsBucket = new ConcurrentHashMap<>();

    public Bucket resolveApiBucket(String userId) {
        return resolveBucket(userId, rateLimitProperties.getApi());
    }

    public Bucket resolveSqlBucket(String userId) {
        return resolveBucket(userId, rateLimitProperties.getSql());
    }

    public Bucket resolveBucket(String userId, BucketProperties bucketProperties) {
        BucketHolder bucketHolder = userId2ApiBucket.computeIfAbsent(userId, t -> newBucketHolder(bucketProperties));
        synchronized (bucketHolder.getLock()) {
            if (!Objects.equal(bucketHolder.getProperties(), bucketProperties)) {
                bucketHolder.setProperties(bucketProperties);
                bucketHolder.setBucket(newBucket(bucketProperties));
            }
        }
        return bucketHolder.getBucket();
    }

    private BucketHolder newBucketHolder(BucketProperties bucketProperties) {
        Bucket bucket = newBucket(bucketProperties);
        return new BucketHolder(bucket, bucketProperties);
    }

    private Bucket newBucket(BucketProperties bucketProperties) {
        Refill refill = Refill.intervally(bucketProperties.getRefillTokens(),
                Duration.ofSeconds(bucketProperties.getRefillDurationSeconds()));
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(bucketProperties.getCapacity(), refill))
                .build();
    }

    @Data
    private static class BucketHolder {
        private Bucket bucket;
        private RateLimitProperties.BucketProperties properties;
        private final Object lock = new Object();

        public BucketHolder() {}

        public BucketHolder(Bucket bucket, BucketProperties properties) {
            this.bucket = bucket;
            this.properties = properties;
        }
    }

}
