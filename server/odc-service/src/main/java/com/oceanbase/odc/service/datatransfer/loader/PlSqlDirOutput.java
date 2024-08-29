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
package com.oceanbase.odc.service.datatransfer.loader;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author youshu
 * @date 2024/8/27
 */
@Slf4j
public class PlSqlDirOutput extends AbstractThirdPartyOutput {

    public PlSqlDirOutput(File origin) {
        super(origin);
    }

    @Override
    public boolean supports() {
        if (!origin.isDirectory()) {
            return false;
        }
        boolean isFromPlSql = false;
        for (File file : origin.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            String[] split = file.getName().split("\\.");
            String suf = split[split.length - 1];
            if (PLSqlDeveloperExportFormat.isPlFileSuffix(suf)) {
                isFromPlSql = true;
                break;
            }
        }
        return isFromPlSql;
    }

    @Override
    public void toObLoaderDumperCompatibleFormat(File dest) throws IOException {
        for (File file : origin.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }

            String filename = file.getName();
            String[] split = filename.split("\\.");
            String suf = split[split.length - 1];

            PLSqlDeveloperExportFormat.ObjectType objectType = PLSqlDeveloperExportFormat.ObjectType.from(suf);
            if (objectType != PLSqlDeveloperExportFormat.ObjectType.UNKNOWN) {
                String directory = objectType.name();
                // point may exist in object name
                String objectName = filename.split("\\." + suf)[0];
                FileUtils.copyFile(file, new File(dest, directory + "/" + objectName + "-schema.sql"));
            }

        }
    }

    @Override
    public String getNewFilePrefix() {
        return "";
    }

}
