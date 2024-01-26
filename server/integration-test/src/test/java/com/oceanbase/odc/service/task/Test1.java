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

import org.junit.Test;

import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.util.JobEncryptUtils;

import cn.hutool.core.lang.Assert;

/**
 * @author yaobin
 * @date 2024-01-25
 * @since 4.2.4
 */
public class Test1 {

    @Test
    public void test1() {
        int port = 8081;
        String key = PasswordUtils.random(32);
        String salt = PasswordUtils.random(8);
        String s1 = JobEncryptUtils.encrypt(key, salt, port + "");

        System.setProperty(JobEnvKeyConstants.ENCRYPT_KEY, key);
        System.setProperty(JobEnvKeyConstants.ENCRYPT_SALT, salt);
        String decrypt = JobEncryptUtils.decrypt(s1);
        Assert.equals(port + "", decrypt);

    }
}
