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
package com.oceanbase.odc.agent;

import com.oceanbase.odc.agent.runtime.TaskApplication;
import com.oceanbase.odc.server.module.Modules;

import lombok.extern.slf4j.Slf4j;

/**
 * main class for Odc agent
 * 
 * @author longpeng.zlp
 * @date 2024/8/9 15:31
 */
@Slf4j
public class OdcAgent {
    public static void main(String[] args) {

        log.info("ODC start as task executor mode");
        try {
            Modules.load();
            new TaskApplication().run(args);
        } catch (Throwable e) {
            log.error("Task existed abnormal", e);
        }
        log.info("Task executor exit.");
    }
}
