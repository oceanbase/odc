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
package com.oceanbase.odc.service.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IntegrationEventHandlerDelegate {

    @Autowired
    private List<IntegrationEventHandler> integrationEventHandlers;

    public void process(IntegrationEvent event) {
        for (IntegrationEventHandler handler : integrationEventHandlers) {
            if (handler.support(event)) {
                if (event.isPreCreate()) {
                    handler.preCreate(event);
                } else if (event.isPreDelete()) {
                    handler.preDelete(event);
                } else if (event.isPreUpdate()) {
                    handler.preUpdate(event);
                }
            }
        }
    }
}
