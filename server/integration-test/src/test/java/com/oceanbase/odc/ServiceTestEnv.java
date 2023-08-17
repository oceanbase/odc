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
package com.oceanbase.odc;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.oceanbase.odc.server.OdcServer;

/**
 * @author yizhou.xw
 * @version : ServiceTestEnv.java, v 0.1 2021-07-23 10:07
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OdcServer.class)
@EnableTransactionManagement
public abstract class ServiceTestEnv extends PluginTestEnv {
    static {
        System.setProperty("DB_PATH", "~");
        System.setProperty("ODC_TASK_TYPE", "");
        System.setProperty("ODC_BEFORE_ACTION", "");
        System.setProperty("ODC_TASK_ACTION", "");
        System.setProperty("ODC_AFTER_ACTION", "");
        System.setProperty("file.storage.dir", "~");
        System.setProperty("obclient.work.dir", "~");
    }
}
