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
package com.oceanbase.odc.service.logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogToolUnit {

    public final static String DEFAULT_LOG_CONTENT = "Read Log Failed";

    public static String readLog(File logFile, long maxLimitedCount, long maxLogSizeCount) {
        return readLog(logFile, maxLimitedCount, maxLogSizeCount, DEFAULT_LOG_CONTENT);
    }

    private static String readLog(File logFile, long maxLimitedCount, long maxLogSizeCount, String defaultContent) {
        if (!FileUtil.exist(logFile)) {
            log.warn("Log file not exist: {}", logFile.getAbsolutePath());
            return defaultContent;
        }
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<>();
            int bytes = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                bytes += line.getBytes().length;
                if (lines.size() >= maxLimitedCount || bytes >= maxLogSizeCount) {
                    lines.add(String.format("[ODC INFO]: \n"
                            + "Logs exceed max limitation (%s rows or %s MB), only the latest part is displayed.\n"
                            + "Please download the log file for the full content.", maxLimitedCount,
                            BinarySizeUnit.convertTo(maxLogSizeCount, BinarySizeUnit.B, BinarySizeUnit.MB)));
                    break;
                }
            }
            StringBuilder logBuilder = new StringBuilder();
            for (int i = lines.size() - 1; i >= 0; i--) {
                logBuilder.append(lines.get(i)).append("\n");
            }
            return logBuilder.toString();
        } catch (Exception ex) {
            log.warn("Read task log file failed, details={}", ex.getMessage());
            throw new UnexpectedException("Read task log file failed, details: " + ex.getMessage(), ex);
        }
    }

}
