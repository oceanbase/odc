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
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

import lombok.NonNull;

/**
 * Decrypt
 * 
 * @author yaobin
 * @date 2024-01-26
 * @since 4.2.4
 */
public class JobDecryptInterceptor implements JobEnvInterceptor {

    @Override
    public void intercept(@NonNull Map<String, String> environments) {

        String key = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_KEY);
        String salt = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_SALT);
        TextEncryptor textEncryptor = Encryptors.aesBase64(key, salt);

        JobEnvInterceptor.getSensitiveKeys().forEach(k -> {
            if (environments.containsKey(k)) {
                System.setProperty(k, textEncryptor.decrypt(environments.get(k)));
            }
        });
    }
}
