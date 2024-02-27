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

package com.oceanbase.odc.service.task.executor.logger;

import java.io.File;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class LogUtils {

    public static final long MAX_LOG_LINE_COUNT = 10000;
    public static final long MAX_LOG_BYTE_COUNT = 1024 * 1024;
    private static final String TASK_LOG_PATH_PATTERN = "%s/task/%s/task-log.%s";

    public static String getLogContent(String file, Long fetchMaxLine, Long fetchMaxByteSize) {

        long lineSize = fetchMaxLine != null ? Math.min(MAX_LOG_LINE_COUNT, fetchMaxLine) : MAX_LOG_LINE_COUNT;
        long byteSize = fetchMaxByteSize != null ? Math.min(MAX_LOG_BYTE_COUNT, fetchMaxByteSize) : MAX_LOG_BYTE_COUNT;
        if (!new File(file).exists()) {
            return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {file});
        }
        LineIterator it = null;
        StringBuilder sb = new StringBuilder();
        int lineCount = 1;
        int byteCount = 0;
        try {
            it = FileUtils.lineIterator(new File(file));
            while (it.hasNext()) {
                if (lineCount > lineSize || byteCount > byteSize) {
                    sb.append("Logs exceed max limitation (10000 rows or 1 MB), please download logs directly");
                    break;
                }
                String line = it.nextLine();
                sb.append(line).append("\n");
                lineCount++;
                byteCount = byteCount + line.getBytes().length;
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("read task log file failed, reason={}", ex.getMessage());
            throw new UnexpectedException("read task log file failed, reason: " + ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(it);
        }
    }

    public static String getTaskLogFileWithPath(Long jobId, OdcTaskLogLevel logType) {
        return String.format(TASK_LOG_PATH_PATTERN, getBaseLogPath(), jobId, logType.getName().toLowerCase());
    }

    public static String getBaseLogPath() {
        return Optional.ofNullable(System.getProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY)).orElse("./log");
    }

}
