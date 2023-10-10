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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.common.util.MapperUtils;
import com.oceanbase.odc.core.migrate.resource.ResourceManager;
import com.oceanbase.odc.core.migrate.resource.ResourceSpecMigrator;
import com.oceanbase.odc.core.migrate.resource.factory.DefaultResourceMapperFactory;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec.DBReference;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec.FieldReference;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec.ValueFromConfig;
import com.oceanbase.odc.core.migrate.resource.repository.DataRecordRepository;
import com.oceanbase.odc.core.migrate.resource.util.PrefixPathMatcher;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ResourceMigrator}
 *
 * @author yh263208
 * @date 2022-04-25 13:49
 * @since ODC_release_3.3.1
 * @see Migrator
 */
@Slf4j
public class ResourceMigrator implements Migrator {

    private String version;
    private String description;
    private String checkSum;
    private final String path;
    private final List<ResourceMigrateMetaInfo> migrateMetas;

    public ResourceMigrator(@NonNull String path, @NonNull InputStream input,
            @NonNull List<ResourceMigrateMetaInfo> migrateMetas) {
        this.path = path;
        this.migrateMetas = migrateMetas;
        init(input);
    }

    private void init(InputStream inputStream) {
        String[] parts = FilenameUtils.removeExtension(path).split("__");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid file name format, path " + path);
        }
        String prefixAndVersion = parts[0];
        if (prefixAndVersion.length() <= 2) {
            throw new IllegalArgumentException("Invalid file name format, version not found, path " + path);
        }
        this.version = StringUtils.replace(StringUtils.substring(prefixAndVersion, 2), "_", ".");
        this.description = StringUtils.replace(parts[1], "_", " ");
        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Validate.notBlank(content, "script content cannot be blank");
            this.checkSum = HashUtils.sha1(content);
        } catch (IOException e) {
            throw new IllegalStateException("Initialize resource migrate failed", e);
        }
    }

    @Override
    public Behavior behavior() {
        return Behavior.fromFileName(path);
    }

    @Override
    public Type type() {
        return Type.RESOURCE;
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
        return this.path;
    }

    @Override
    public String checksum() {
        return this.checkSum;
    }

    @Override
    public boolean doMigrate() {
        log.info("mark!!!!!!!!!!!");
        log.info(System.getProperty("ODC_UT_SECRET", "not found"));
        log.info(System.getProperty("ODC_CONFIG_SECRET", "not found"));
        for (ResourceMigrateMetaInfo migrateMeta : migrateMetas) {
            ResourceManager manager = migrateMeta.getManager();
            ResourceConfig config = migrateMeta.getConfig();
            ResourceSpecMigrator migrator = new ResourceSpecMigrator(
                    new DataRecordRepository(config.getDataSource()),
                    new DefaultResourceMapperFactory(config, manager), config.getHandle());
            ResourceSpec resourceSpec = findResourceSpec(manager, path);
            resourceSpec.getTemplates().stream().flatMap(t -> t.getSpecs().stream())
                    .forEach(t -> fullFillFieldReference(manager, migrator, t.getValueFrom()));
            migrator.migrate(resourceSpec);
        }
        return true;
    }

    private void fullFillFieldReference(ResourceManager manager,
            ResourceSpecMigrator migrator, ValueFromConfig valueFromConfig) {
        if (valueFromConfig == null) {
            return;
        }
        FieldReference fieldRef = valueFromConfig.getFieldRef();
        if (fieldRef == null || StringUtils.isBlank(fieldRef.getRefFile())) {
            DBReference dbReference = valueFromConfig.getDbRef();
            if (dbReference != null) {
                dbReference.getFilters().forEach(t -> fullFillFieldReference(manager, migrator, t.getValueFrom()));
            }
            return;
        }
        ResourceSpec target = findResourceSpec(manager, fieldRef.getRefFile());
        Object obj = MapperUtils.get(target, Object.class, new PrefixPathMatcher(fieldRef.getFieldPath()));
        if (obj instanceof List && ((List<?>) obj).stream().anyMatch(Objects::isNull)) {
            target.getTemplates().stream().flatMap(t -> t.getSpecs().stream())
                    .forEach(t -> fullFillFieldReference(manager, migrator, t.getValueFrom()));
            migrator.refresh(target);
        } else if (obj == null) {
            target.getTemplates().stream().flatMap(t -> t.getSpecs().stream())
                    .forEach(t -> fullFillFieldReference(manager, migrator, t.getValueFrom()));
            migrator.refresh(target);
        }
    }

    private ResourceSpec findResourceSpec(ResourceManager manager, String path) {
        List<ResourceSpec> entities = manager.findBySuffix(path);
        if (entities.size() != 1) {
            log.warn("Resource not available by suffix, suffix={}", path);
            throw new IllegalStateException("Resource not available by suffix");
        }
        return entities.get(0);
    }

    @Getter
    static class ResourceMigrateMetaInfo {
        private final ResourceConfig config;
        private final ResourceManager manager;

        public ResourceMigrateMetaInfo(@NonNull ResourceConfig config, @NonNull ResourceManager manager) {
            this.config = config;
            this.manager = manager;
        }
    }

}
