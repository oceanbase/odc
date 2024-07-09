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
package com.oceanbase.tools.dbbrowser.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import javax.validation.constraints.NotBlank;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/7/8
 */
@Slf4j
public class TimestampUtils {
    public static Timestamp convertToDefaultTimeZone(@NonNull Timestamp timestamp, @NotBlank String timeZone) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeString = sdf.format(timestamp);
        String defaultTimeZone = TimeZone.getDefault().getID();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(timeString, formatter);

        // 指定输入时区
        ZonedDateTime inputZonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of(timeZone));
        // 将输入时区转换为输出时区
        ZonedDateTime outputZonedDateTime = inputZonedDateTime.withZoneSameInstant(ZoneId.of(defaultTimeZone));

        return Timestamp.valueOf(outputZonedDateTime.toLocalDateTime());
    }
}
