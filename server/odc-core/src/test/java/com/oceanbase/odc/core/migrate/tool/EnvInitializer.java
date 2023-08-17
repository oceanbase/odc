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
package com.oceanbase.odc.core.migrate.tool;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.NonNull;

public class EnvInitializer {

    public static void init(@NonNull DataSource dataSource) throws IOException {
        URL url = EnvInitializer.class.getClassLoader().getResource("migrate/resource/init_resource.sql");
        Validate.notNull(url);
        String content = FileUtils.readFileToString(new File(url.getPath()));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(content);
    }
}
