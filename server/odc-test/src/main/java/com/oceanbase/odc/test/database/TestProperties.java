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
package com.oceanbase.odc.test.database;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.test.tool.EncryptableConfigurations;

/**
 * @author gaoda.xy
 * @date 2023/2/16 17:19
 */
public class TestProperties {

    private static final Map<String, String> properties;

    static {
        try {
            // 获取当前类的保护域的代码源的位置
            URL location = TestProperties.class.getProtectionDomain().getCodeSource().getLocation();
            // 获取代码源的URI，并将其转换为路径
            Path filepath = Paths.get(location.toURI()).getParent().getParent().getParent().getParent()
                    .resolve("local-unit-test.properties");
            // 如果指定的属性文件存在，则加载其中的配置项
            if (Files.exists(filepath)) {
                properties = EncryptableConfigurations.loadProperties(filepath.toString());
            } else {
                // 否则创建一个空的属性列表
                properties = new HashMap<>();
            }
        } catch (URISyntaxException e) {
            // 如果出现语法错误，则抛出异常
            throw new IllegalStateException(e);
        }
    }

    /**
     * 获取属性值
     *
     * @param key 属性键
     * @return 属性值
     */
    public static String getProperty(String key) {
        if (key == null) {
            return null;
        }
        String property = properties.get(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        // We prefer to use "." in property key, but "." is not allowed in environment variable
        // 将"."替换为"_"，并转换为大写，因为环境变量不允许使用"."
        key = StringUtils.replace(key, ".", "_").toUpperCase();
        property = EncryptableConfigurations.getDecryptedProperty(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        return null;
    }

}
