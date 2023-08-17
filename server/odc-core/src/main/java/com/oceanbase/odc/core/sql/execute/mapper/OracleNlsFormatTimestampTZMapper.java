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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.jdbc.extend.datatype.TIMESTAMPTZ;
import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.core.sql.util.OracleTimestampFormat;
import com.oceanbase.odc.core.sql.util.TimeZoneUtil;

import lombok.NonNull;

/**
 * {@link OracleNlsFormatTimestampTZMapper}
 *
 * @author yh263208
 * @date 2023-07-04 21:02
 * @since ODC_release_4.2.0
 */
public class OracleNlsFormatTimestampTZMapper extends OracleGeneralTimestampTZMapper {

    private final OracleTimestampFormat format;

    public OracleNlsFormatTimestampTZMapper(@NonNull String pattern) {
        this.format = new OracleTimestampFormat(pattern, TimeZone.getDefault(), LocaleContextHolder.getLocale(), true);
    }

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        try {
            return map(data);
        } catch (Exception e) {
            Object obj = super.mapCell(data);
            return obj == null ? null : new TimeFormatResult(obj.toString());
        }
    }

    private Object map(CellData cellData) throws SQLException {
        Object obj = cellData.getObject();
        if (obj == null) {
            return null;
        }
        byte[] bytes = ((TIMESTAMPTZ) obj).toBytes();
        String timeZoneId = getTimeZoneId(bytes);
        if (!TimeZoneUtil.isValid(timeZoneId)) {
            /**
             * 这里 driver 有 bug，具体来说就是 timestamp with time zone 类型的时区信息是在 jdbc 协议中透传的，通常是这样的一个值
             * {@code +8:00}。driver 会尝试将这个 timeZoneId 通过 {@link TimeZone#getTimeZone(String)} 转成
             * {@link TimeZone}，然而 {@code +8:00} 是不符合 JDK 对时区 ID 的定义的，此时就会转成一个 GMT+0 的时区，driver
             * 会不加分辨地直接用这个错误的时区初始化时间，得到的时间戳（毫秒值）自然是错误的。
             *
             * 但是，经过实际测试发现通过 driver 获取的时间字符串又是正确的，这是为什么呢？原因在于我们最终拼成的字符串是类似这样的形式：
             * 
             * <pre>
             *     2023-07-03 12:12:12 +8:00
             * </pre>
             * 
             * 最后的时区由于是直接从协议中拿到的所以肯定不会错。前面的具体日期是由错误的时间戳（毫秒值）转成 Java 对象再转回字符串的，driver 这里的做法是将错误的时间戳 set 进
             * {@link java.util.Calendar} 然后再给 {@link java.util.Calendar}
             * 设时区（当然，这里的时区依然是错误的），由于时间戳是错误的，时区也是错误的，两个错误错到一起去了，反而导致最终获取的时间戳字符串正确了。
             *
             * ODC 这里不能受这个 bug 的影响，因为 ODC 是需要给前端返回正确的时区信息的。
             */
            if (TimeZoneUtil.isValid("GMT" + timeZoneId)) {
                timeZoneId = "GMT" + timeZoneId;
            } else {
                timeZoneId = TimeZoneUtil.createCustomTimeZoneId(timeZoneId);
                if (timeZoneId == null) {
                    return super.mapCell(cellData);
                }
            }
        }
        Long timestamp = getTimeMillis(bytes, timeZoneId);
        Integer nano = getNanos(bytes);

        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        this.format.setTimeZone(timeZone);
        Timestamp ts = new Timestamp(timestamp);
        ts.setNanos(nano);
        if (StringUtils.startsWithIgnoreCase(timeZone.getID(), "GMT")) {
            timeZoneId = timeZone.getID().substring(3).trim();
        }
        return new TimeFormatResult(this.format.format(ts), timestamp, nano, timeZoneId);
    }

}
