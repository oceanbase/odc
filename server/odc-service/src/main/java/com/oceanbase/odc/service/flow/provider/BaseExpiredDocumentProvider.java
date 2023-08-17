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
package com.oceanbase.odc.service.flow.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseExpiredDocumentProvider {

    private final long fileExpireMillis;

    public BaseExpiredDocumentProvider(@NonNull int fileExpireHours) {
        this(fileExpireHours, TimeUnit.HOURS);
    }

    public BaseExpiredDocumentProvider(@NonNull long fileExpireTime, @NonNull TimeUnit timeUnit) {
        Verify.verify(fileExpireTime > 0, "FileExpireTime can not be negative");
        this.fileExpireMillis = TimeUnit.MILLISECONDS.convert(fileExpireTime, timeUnit);
    }

    public List<File> provide() {
        File file;
        try {
            file = getRootScanDir();
            if (!file.exists()) {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn("Failed to get root scan dir", e);
            return Collections.emptyList();
        }
        Verify.verify(file.isDirectory(), "Target file has to be a dir " + file.getAbsolutePath());
        try {
            return getAllFiles(file, new ExpiredFilePredicate(System.currentTimeMillis() - fileExpireMillis));
        } catch (FileNotFoundException e) {
            log.warn("Failed to get expire file", e);
            throw new IllegalStateException(e);
        }
    }

    abstract protected File getRootScanDir() throws IOException;

    private static List<File> getAllFiles(File rootDir, Predicate<File> predicate) throws FileNotFoundException {
        if (!rootDir.exists()) {
            throw new FileNotFoundException(rootDir.getAbsolutePath());
        }
        List<File> returnVal = new LinkedList<>();
        if (rootDir.isFile()) {
            if (predicate.test(rootDir)) {
                returnVal.add(rootDir);
                return returnVal;
            }
            return Collections.emptyList();
        } else {
            for (File subFile : rootDir.listFiles()) {
                if (subFile.isFile()) {
                    if (predicate.test(subFile)) {
                        returnVal.add(subFile);
                    }
                } else {
                    if (predicate.test(subFile)) {
                        returnVal.add(subFile);
                    }
                    returnVal.addAll(getAllFiles(subFile, predicate));
                }
            }
            return returnVal;
        }
    }

    public static class ExpiredFilePredicate implements Predicate<File> {

        private final long expireTime;

        public ExpiredFilePredicate(@NonNull long expireTime) {
            this.expireTime = expireTime;
        }

        @Override
        public boolean test(File file) {
            if (!file.isFile()) {
                return false;
            }
            return file.lastModified() < expireTime;
        }
    }
}
