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
package com.oceanbase.odc.service.connection;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.service.connection.listener.UpdateDatasourceListener;

import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2024/12/30 10:57
 * @Descripition:
 */

@Component
public class ConnectionEventPublisher {
    @Autowired
    private LocalEventPublisher localEventPublisher;

    @Autowired
    private UpdateDatasourceListener updateDatasourceListener;

    @PostConstruct
    public void init() {
        localEventPublisher.addEventListener(updateDatasourceListener);
    }

    public <T extends AbstractEvent> void publishEvent(@NonNull T event) {
        localEventPublisher.publishEvent(event);
    }

}
