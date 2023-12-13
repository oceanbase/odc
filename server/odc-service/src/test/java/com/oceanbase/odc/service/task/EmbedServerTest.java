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

package com.oceanbase.odc.service.task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.task.executor.EmbedServer;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class EmbedServerTest {

    @Test
    public void test_server() throws Exception {

        EmbedServer server = new EmbedServer();
        server.start(8888);

        synchronized (this) {
            this.wait(100000000);
        }
    }

    @Test
    public void test_path() throws Exception{
        Pattern p = Pattern.compile("/([0-9]+)/tasks/log");
        Matcher matcher = p.matcher("/1233/tasks/log");
        if (matcher.find()) {
            log.info("found : " + matcher.group(1));
        }

    }
}
