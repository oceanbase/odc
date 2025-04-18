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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FutureCache {

    private final Cache<String, Future<?>> tempId2Future =
            Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES)
                    .removalListener((String key, Future<?> future, RemovalCause cause) -> {
                        if (future != null) {
                            future.cancel(true);
                        }
                        log.info("Remove future cause={}, futureKey={},", cause, key);
                    })
                    .build();

    public void put(String id, Future<?> future) {
        tempId2Future.put(id, future);
    }

    public Future<?> get(String id) {
        return tempId2Future.get(id, k -> null);
    }

    public void invalid(String id) {
        tempId2Future.invalidate(id);
    }
}
