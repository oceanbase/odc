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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;

public class LocalResourceFinder implements ResourceFinder<Resource> {
    private static final Pattern DATA_FILE_PATTERN =
            Pattern.compile("^\"?([^\\-\\.]+)\"?(\\.[0-9]+){0,2}\\.(sql|csv|dat|txt)$", Pattern.CASE_INSENSITIVE);

    private final DataTransferConfig transferConfig;
    private final File workingDir;

    public LocalResourceFinder(DataTransferConfig transferConfig, File workingDir) {
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
    }

    @Override
    public List<Resource> listSchemaResources() throws Exception {
        Verify.verify(transferConfig.isCompressed(), "External file is not supported.");

        return filterLocalFiles(workingDir, file -> {
            // xxx-schema.sql
            int index = file.getName().indexOf(Constants.DDL_SUFFIX);
            return index > 0 ? file.getName().substring(0, index) : "";
        });
    }

    @Override
    public List<Resource> listRecordResources() throws Exception {
        Verify.verify(transferConfig.isCompressed(), "External file is not supported.");

        return filterLocalFiles(workingDir, file -> {
            // filter schema files
            if (file.getName().indexOf(Constants.DDL_SUFFIX) > 0) {
                return "";
            }
            Matcher matcher = DATA_FILE_PATTERN.matcher(file.getName());
            return matcher.matches() ? matcher.group(1) : "";
        });
    }

    private List<Resource> filterLocalFiles(File directory, Function<File, String> nameExtractor)
            throws IOException {
        if (!directory.isDirectory()) {
            throw new FileNotFoundException(directory.getAbsolutePath());
        }
        List<Resource> result = new ArrayList<>();
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                File file = path.toFile();
                if (!file.isHidden() && file.isFile()) {
                    String objectName = nameExtractor.apply(file);
                    if (StringUtils.isNotEmpty(objectName)) {
                        result.add(new LocalResource(path, objectName, file.getParentFile().getName().toUpperCase()));
                    }
                }
                return super.visitFile(path, attrs);
            }
        });
        return result;
    }

}
