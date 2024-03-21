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

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link JdbcColumnMapper} for data type {@code timestamp[0-9]}
 *
 * @author yh263208
 * @date 2022-06-28 11:54
 * @since ODC_release_3.4.0
 * @see JdbcColumnMapper
 */
public class MySQLTimestampMapper implements JdbcColumnMapper {

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        Timestamp timestamp = data.getTimestamp();
        if (timestamp == null) {
            return null;
        }
        if (!isValidTimestamp(data)) {
            return "0000-00-00 00:00:00";
        }

        String returnValue = new String (data.getBytes()) ;
       /* if (returnValue.endsWith(".0")) {
            return returnValue.substring(0, returnValue.length() - 2);
        }*/
        return returnValue;
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "TIMESTAMP".equalsIgnoreCase(dataType.getDataTypeName());
    }

    /**
     * 如果 {@code sql_mode} 中允许非法的时间插入，类似于这样的时间：
     *
     * <code>
     *     create table datetime_tb(c datetime);
     *     insert into datetime_tb values(0);
     * </code>
     *
     * 在获取到这样的非法时间时，driver 存在相应的参数{@code zeroDateTimeBehavior}控制此时的行为：
     *
     * <pre>
     *     1. 默认：抛出错误
     *     2. null: 转化为 null
     *     3. round: 将不合法的日期转化为一个相近的日期
     * </pre>
     *
     * 目前来看，设置为{@code round}是比较合适的。
     *
     * 在这种设置下，如果此时是一个非法的日期，driver 的行为是会返回{@code 0001-01-01 00:00:00}，这个和用户的预期不符，navicat
     * 返回{@code 0000-00-00 00:00:00}，所以这里还是跟 navicat 保持一致。
     *
     * 需要注意的是：这个只影响到 timestamp 类型的，date 不影响，这可能是 driver 为了兼容 mysql-driver 的行为，否则 timestamp 上 getString
     * 的行为完全可以类似于 date：如果发现协议中的内容是非法的就直接把协议中的值原样返回，而不必再去根据{@code zeroDateTimeBehavior}处理。
     */
    protected boolean isValidTimestamp(CellData data) throws SQLException {
        byte[] buffer = data.getBytes();
        if (buffer == null) {
            throw new NullPointerException("Timestamp is null");
        }
        String stringValue = new String(buffer);
        return stringValue.length() <= 0
                || stringValue.charAt(0) != '0'
                || !"0000-00-00".equals(stringValue)
                        && !"0000-00-00 00:00:00".equals(stringValue)
                        && !"00000000000000".equals(stringValue)
                        && !"0".equals(stringValue);
    }

}

