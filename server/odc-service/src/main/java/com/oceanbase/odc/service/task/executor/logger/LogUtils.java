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
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;

import org.apache.commons.io.input.ReversedLinesFileReader;

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

    public static String getLogContentReversed(String file, Long fetchMaxLine, Long fetchMaxByteSize) {

        File logFile = new File(file);
        if (!logFile.exists()) {
            return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {file});
        }
        LinkedList<String> logContent = new LinkedList<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            int bytes = 0;
            int lineCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                bytes += line.getBytes().length;
                if (lineCount >= fetchMaxLine || bytes >= fetchMaxByteSize) {
                    logContent.addFirst("[ODC INFO]: \n"
                            + "Logs exceed max limitation (10000 rows or 1 MB), only the latest part is displayed.\n"
                            + "please download logs directly.");
                    break;
                }
                logContent.addFirst(line + "\n");
                lineCount++;
            }

        } catch (Exception ex) {
            log.warn("Read task log file failed, details={}", ex.getMessage());
            throw new UnexpectedException("Read task log file failed, details: " + ex.getMessage(), ex);
        }
        StringBuilder logBuilder = new StringBuilder();
        logContent.forEach(logBuilder::append);
        return logBuilder.toString();
    }

    public static String getTaskLogFileWithPath(Long jobId, OdcTaskLogLevel logType) {
        return String.format(TASK_LOG_PATH_PATTERN, getBaseLogPath(), jobId, logType.getName().toLowerCase());
    }

    public static String getBaseLogPath() {
        return Optional.ofNullable(System.getProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY)).orElse("./log");
    }

}
