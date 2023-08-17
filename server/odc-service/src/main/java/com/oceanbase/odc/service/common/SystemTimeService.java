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

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;

/**
 * System time service, make current time method easy to mock
 * 
 * @author yizhou.xw
 * @version : SystemTimeService.java, v 0.1 2021-7-28 21:50
 */
@Service
@SkipAuthorize("odc internal usage")
public class SystemTimeService {

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long currentTimeSeconds() {
        return currentTimeMillis() / 1000L;
    }

    public OffsetDateTime nowOffsetDateTime() {
        return OffsetDateTime.now();
    }

}
