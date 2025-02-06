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
package com.oceanbase.odc.service.quartz;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;

/**
 * @author jingtian
 * @date 2024/12/25
 */
@Service("quartzJobService")
@SkipAuthorize("odc internal usage")
public class QuartzJobService extends AbstractQuartzJobService {
    @Autowired(required = false)
    @Qualifier(value = ("commonScheduler"))
    private Scheduler commonScheduler;

    @Override
    protected Scheduler getScheduler() {
        return commonScheduler;
    }
}
