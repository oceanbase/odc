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
package com.oceanbase.odc.service.task.jasypt;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

import lombok.Data;

/**
 * @author yaobin
 * @date 2024-04-01
 * @since 4.2.4
 */
@Data
public class AccessEnvironmentJasyptEncryptorConfigProperties implements JasyptEncryptorConfigProperties {

    private String prefix;
    private String suffix;
    private String algorithm;
    private String salt;

    public AccessEnvironmentJasyptEncryptorConfigProperties() {
        initFromEnvironment();
    }

    private void initFromEnvironment() {
        setPrefix(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_PREFIX));
        setSuffix(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SUFFIX));
        setAlgorithm(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM));
        setSalt(System.getProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT));
    }
}
