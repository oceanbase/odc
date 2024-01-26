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
package com.oceanbase.odc.service.task.caller;

import java.util.Map;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2024-01-26
 * @since 4.2.4
 */
public class JobEncryptInterceptor implements JobEnvInterceptor {

    private final String key = PasswordUtils.random(32);
    private final String salt = PasswordUtils.random(8);
    private final TextEncryptor textEncryptor = Encryptors.aesBase64(key, salt);

    /**
     * this will encrypt all keys inside JobEnvInterceptor#getSensitiveKeys()
     * 
     * @param environments all environments be passed to task executor
     */
    @Override
    public void intercept(@NonNull Map<String, String> environments) {

        JobEnvInterceptor.getSensitiveKeys().forEach(key -> {
            environments.computeIfPresent(key, (k, v) -> textEncryptor.encrypt(v));
        });

        environments.putIfAbsent(JobEnvKeyConstants.ENCRYPT_KEY, key);
        environments.putIfAbsent(JobEnvKeyConstants.ENCRYPT_SALT, salt);
    }
}
