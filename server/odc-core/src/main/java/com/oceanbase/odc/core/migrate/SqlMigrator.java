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
package com.oceanbase.odc.core.migrate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.oceanbase.odc.common.util.HashUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : SqlMigrator.java, v 0.1 2021-03-26 18:13
 */
@Slf4j
class SqlMigrator implements Migrator {
    private final String fileName;
    private final DataSource dataSource;
    private final String content;
    private String version;
    private String description;
    private String checksum;

    SqlMigrator(String fileName, InputStream inputStream, DataSource dataSource) {
        Validate.notEmpty(fileName, "parameter fileName may not be empty");
        Validate.notNull(inputStream, "parameter inputStream may not be null");
        Validate.notNull(dataSource, "parameter dataSource may not be null");
        this.fileName = fileName;
        this.dataSource = dataSource;
        try {
            this.content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Validate.notBlank(this.content, "script content cannot be blank");
            initialize();
        } catch (IOException e) {
            throw new RuntimeException("initialize sql migrate failed", e);
        }
    }

    private void initialize() throws IOException {
        String extension = FilenameUtils.getExtension(fileName);
        Validate.isTrue(StringUtils.equalsIgnoreCase("sql", extension),
                String.format("invalid file extension '%s'", extension));

        String name = FilenameUtils.removeExtension(fileName);
        String[] parts = name.split("__");
        Validate.isTrue(2 == parts.length,
                String.format("invalid file name format, only one '__' expected in fileName, fileName=%s", fileName));

        String prefixAndVersion = parts[0];
        Validate.isTrue(prefixAndVersion.length() > 2,
                String.format("invalid file name format, version not found, fileName=%s", fileName));

        this.version = StringUtils.replace(StringUtils.substring(prefixAndVersion, 2), "_", ".");
        this.description = StringUtils.replace(parts[1], "_", " ");
        this.checksum = HashUtils.sha1(content);
    }

    @Override
    public Behavior behavior() {
        return Behavior.fromFileName(fileName);
    }

    @Override
    public Type type() {
        return Type.SQL;
    }

    @Override
    public String version() {
        return this.version;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String script() {
        return this.fileName;
    }

    @Override
    public String checksum() {
        return this.checksum;
    }

    @Override
    public boolean doMigrate() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        InputStreamResource resource = new InputStreamResource(inputStream);
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(resource);
        databasePopulator.setSqlScriptEncoding("UTF-8");
        databasePopulator.execute(dataSource);
        IOUtils.closeQuietly(inputStream);
        return true;
    }
}
