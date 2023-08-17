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
package com.oceanbase.odc.service.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/3/21
 */

@Slf4j
public class FileRecycler implements Runnable {
    private static Long DEFAULT_FILE_RESERVING_MILLIS = 48 * 60 * 60 * 1000L;// 48h
    private File monitorDirectory;
    private Long fileReservingMillis;

    public FileRecycler(String monitorDirectoryStr) {
        this(monitorDirectoryStr, DEFAULT_FILE_RESERVING_MILLIS);
    }

    public FileRecycler(String monitorDirectoryStr, Long fileReservingMillis) {
        this.monitorDirectory = new File(monitorDirectoryStr);
        this.fileReservingMillis = fileReservingMillis;
    }

    @Override
    public void run() {
        File[] subDirs = monitorDirectory.listFiles();
        for (File subDir : subDirs) {
            if (subDir.isDirectory()) {
                detect(subDir);
            }
        }
    }

    private void detect(File subDir) {
        BasicFileAttributes attrs;
        for (File file : subDir.listFiles()) {
            try {
                attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                Long createTimestamp = attrs.creationTime().toMillis();
                if (System.currentTimeMillis() - createTimestamp > fileReservingMillis) {
                    log.info("File {} is to be delete due to its existing time over {}mins", file.getName(),
                            fileReservingMillis / (1000 * 60));
                    file.delete();
                }
            } catch (IOException e) {
                log.error("Error occurs when acquiring source file attributes", e);
            }
        }
    }
}
