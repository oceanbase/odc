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

    public static String getSystemEnvProperty(@NonNull String key) {
        String property = System.getProperty(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        property = System.getenv(key);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        return null;
    }

    public static String getDotEnvProperties(@NonNull String key) {
        return getDotEnvProperties().getProperty(key);
    }

    public static Properties getDotEnvProperties() {
        Properties properties = new Properties();
        File file;
        try {
            URL location = TestProperties.class.getProtectionDomain().getCodeSource().getLocation();
            file = Paths.get(location.toURI())
                    .getParent().getParent().getParent().getParent()
                    .resolve(".env").toFile();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        if (file.exists()) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
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
