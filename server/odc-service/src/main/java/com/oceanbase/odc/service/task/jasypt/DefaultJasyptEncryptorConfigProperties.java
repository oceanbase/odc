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

package com.oceanbase.odc.service.task.jasypt;

import com.ulisesbocchio.jasyptspringboot.properties.JasyptEncryptorConfigurationProperties;
import com.ulisesbocchio.jasyptspringboot.util.Singleton;

import lombok.Data;

/**
 * @author yaobin
 * @date 2024-04-01
 * @since 4.2.4
 */
@Data
public class DefaultJasyptEncryptorConfigProperties implements JasyptEncryptorConfigProperties {

    private String prefix;
    private String suffix;
    private String algorithm;
    private String salt;

    public DefaultJasyptEncryptorConfigProperties(
            Singleton<JasyptEncryptorConfigurationProperties> configPropertiesSingleton) {
        initFromConfiguration(configPropertiesSingleton);
    }

    public void initFromConfiguration(
            Singleton<JasyptEncryptorConfigurationProperties> configPropertiesSingleton) {
        JasyptEncryptorConfigurationProperties configProperties = configPropertiesSingleton.get();
        setPrefix(configProperties.getProperty().getPrefix());
        setSuffix(configProperties.getProperty().getSuffix());
        setAlgorithm(configProperties.getAlgorithm());
        setSalt(configProperties.getPassword());
    }

}
