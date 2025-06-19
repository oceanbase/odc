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
package com.oceanbase.odc.service.task.resource;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author longpeng.zlp
 * @date 2025/4/29 09:59
 */
public class K8sPodResourceTest {
    @Test
    public void testParseIpAndPort() {
        String ipAndPortStr =
                "k8s::cn-shanghai::default::obc:aliyun:iaas:cn-shanghai:oceanbase:pod:p-T5205qKf9N0001::null::null";
        Pair<String, String> ret = K8sPodResource.parseIPAndPort(ipAndPortStr);
        Assert.assertNull(ret.getLeft());
        Assert.assertNull(ret.getRight());
    }
}
