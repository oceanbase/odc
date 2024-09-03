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
package com.oceanbase.odc.service.resource.local;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceState;

/**
 * resource in local, in process mode
 * 
 * @author longpeng.zlp
 * @date 2024/8/13 10:25
 */

public class LocalResource implements Resource<ProcessResourceID> {
    private static final AtomicLong ALLOCATE_SEQ = new AtomicLong(0);
    private final long seq = ALLOCATE_SEQ.getAndIncrement();
    private final Date createDate = new Date(System.currentTimeMillis());

    @Override
    public ProcessResourceID id() {
        return new ProcessResourceID(String.valueOf(seq));
    }


    @Override
    public ResourceState resourceState() {
        return ResourceState.RUNNING;
    }

    public Date createDate() {
        return createDate;
    }
}
