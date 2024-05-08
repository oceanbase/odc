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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2024-01-26
 * @since 4.2.4
 */
public class JobEnvironmentEncryptor {

    private final AtomicBoolean ENCRYPTED = new AtomicBoolean(false);

    private final List<String> sensitiveKeys = Arrays.asList(
            JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_HOST,
            JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PORT,
            JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_NAME,
            JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_USERNAME,
            JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PASSWORD,
            JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
            JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT,
            JobEnvKeyConstants.ODC_JOB_CONTEXT);


    /**
     * this will encrypt all sensitiveKeys, this method must be called only once before start job
     * 
     * @param environments all environments be passed to task executor
     */
    public void encrypt(@NonNull Map<String, String> environments) {
        if (ENCRYPTED.compareAndSet(false, true)) {
            String key = PasswordUtils.random(32);
            String salt = PasswordUtils.random(8);
            TextEncryptor textEncryptor = Encryptors.aesBase64(key, salt);

            sensitiveKeys.forEach(sk -> {
                environments.computeIfPresent(sk, (k, v) -> textEncryptor.encrypt(v));
            });

            environments.putIfAbsent(JobEnvKeyConstants.ENCRYPT_KEY, key);
            environments.putIfAbsent(JobEnvKeyConstants.ENCRYPT_SALT, salt);
        }
    }

    /**
     * this will encrypt all sensitiveKeys, this method must be called once before task start
     *
     * @param environments all environments be passed to task executor
     */
    public void decrypt(@NonNull Map<String, String> environments) {

        String key = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_KEY);
        String salt = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_SALT);
        TextEncryptor textEncryptor = Encryptors.aesBase64(key, salt);

        sensitiveKeys.forEach(k -> {
            if (environments.containsKey(k)) {
                System.setProperty(k, textEncryptor.decrypt(environments.get(k)));
            }
        });
    }


}
