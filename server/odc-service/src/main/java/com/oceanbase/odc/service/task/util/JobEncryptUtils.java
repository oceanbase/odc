/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.task.util;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

/**
 * @author yaobin
 * @date 2024-01-19
 * @since 4.2.4
 */
public class JobEncryptUtils {


    public static String decrypt(String encrypted) {
        String key = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_KEY);
        String salt = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ENCRYPT_SALT);
        TextEncryptor textEncryptor = Encryptors.aesBase64(key, salt);
        return textEncryptor.decrypt(encrypted);
    }
}
