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

package com.oceanbase.odc.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.test.database.TestProperties;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/9/27 14:06
 */
@Slf4j
public class PropertiesUtil {

    /**
     * 获取系统环境属性值
     *
     * @param key 属性键
     * @return 属性值，若不存在则返回null
     */
    public static String getSystemEnvProperty(@NonNull String key) {
        // 获取系统属性值
        String property = System.getProperty(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        // 获取系统环境变量值
        property = System.getenv(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        return null;
    }

    public static String getDotEnvProperties(@NonNull String key) {
        return getDotEnvProperties().getProperty(key);
    }

    /**
     * 获取.env文件中的属性
     *
     * @return Properties对象，包含.env文件中的属性
     */
    public static Properties getDotEnvProperties() {
        Properties properties = new Properties();
        File file;
        try {
            // 获取当前类的protection domain的code source的location
            URL location = TestProperties.class.getProtectionDomain().getCodeSource().getLocation();
            // 获取location对应的文件路径的父级父级父级父级目录下的.env文件
            file = Paths.get(location.toURI())
                    .getParent().getParent().getParent().getParent()
                    .resolve(".env").toFile();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        if (file.exists()) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                // 加载.env文件中的属性到Properties对象中
                properties.load(inputStream);
            } catch (IOException e) {
                log.warn("load .env failed, reason={}", e.getMessage());
            }
        } else {
            log.info("skip load due .env file not exists");
        }
        return properties;
    }

}
