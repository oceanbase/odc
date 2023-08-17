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
package com.oceanbase.odc.service.automation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.service.automation.model.TriggerEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TriggerEventListener implements ApplicationListener<TriggerEvent> {

    @Autowired
    private List<TriggerEventHandler> triggerEventHandlers;

    /**
     * handle event not in transaction
     * 
     * @param event the event to respond to
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(TriggerEvent event) {
        for (TriggerEventHandler handler : triggerEventHandlers) {
            if (handler.support(event)) {
                try {
                    handler.handle(event);
                } catch (Exception ex) {
                    log.warn("Handle event {} failed, reason={}", event.getEventName(), ex);
                }
            }
        }
    }

}
