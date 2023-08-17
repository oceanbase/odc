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
package com.oceanbase.odc.core.sql.util;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Pattern;

import com.oceanbase.jdbc.Blob;
import com.oceanbase.jdbc.Clob;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/6/21
 */

@Slf4j
public class JdbcDataTypeUtil {

    public static final String NULL_VALUE = "NULL";
    public static final String TRUE_VALUE = "true";
    public static final String TRUE_BIT_VALUE = "1";
    public static final String OPEN_PARENTHESIS = "(";
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");


    public static boolean validateInParameter(DBPLParam plParam) {
        String dataType = plParam.getDataType();
        String value = plParam.getDefaultValue();
        if (DBPLParamMode.IN == plParam.getParamMode()) {
            return validateValueWithType(dataType, value);
        } else if (DBPLParamMode.INOUT == plParam.getParamMode() && StringUtils.isNotBlank(value)) {
            return validateValueWithType(dataType, value);
        }
        return true;
    }

    public static void setValueIntoStatement(CallableStatement statement, int index, String type, String value)
            throws SQLException, ParseException {
        PreConditions.notEmpty(type, "PLParamDataType");
        JDBCType jdbcType = parseDataType(type);
        // deal with null and '' value
        if (StringUtils.isEmpty(value)) {
            statement.setNull(index, Types.NULL);
            return;
        }
        switch (jdbcType) {
            case BIT:
            case BOOLEAN:
                if (TRUE_BIT_VALUE.equals(value.trim())) {
                    // accept 1 as true
                    value = TRUE_VALUE;
                }
                statement.setBoolean(index, Boolean.valueOf(value).booleanValue());
                break;
            case TINYINT:
                statement.setByte(index, Byte.valueOf(value).byteValue());
                break;
            case SMALLINT:
                statement.setShort(index, Short.valueOf(value).shortValue());
                break;
            case INTEGER:
                statement.setInt(index, Integer.valueOf(value).intValue());
                break;
            case BIGINT:
                statement.setLong(index, Long.valueOf(value).longValue());
                break;
            case REAL:
            case FLOAT:
                statement.setFloat(index, Float.valueOf(value).floatValue());
                break;
            case DOUBLE:
                statement.setDouble(index, Double.valueOf(value).doubleValue());
                break;
            case DECIMAL:
            case NUMERIC:
                statement.setBigDecimal(index, new BigDecimal(value));
                break;
            case ROWID:
            case CHAR:
            case VARCHAR:
            case NVARCHAR:
            case NCHAR:
            case LONGNVARCHAR:
                statement.setString(index, value);
                break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
                // may involve charset
                statement.setBytes(index, value.getBytes());
                break;
            case BLOB:
                statement.setBlob(index, new Blob(value.getBytes()));
            case CLOB:
                statement.setClob(index, new Clob(value.getBytes()));
            case DATE:
                SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat(value));
                java.util.Date date = sdf.parse(value);
                statement.setDate(index, new Date(date.getTime()));
                break;
            case TIME:
                statement.setTime(index, Time.valueOf(value));
                break;
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                statement.setTimestamp(index, Timestamp.valueOf(value));
                break;
            default:
                statement.setString(index, value);
        }
    }

    public static Object getValueFromStatement(CallableStatement statement, int index, String type)
            throws SQLException {
        JDBCType jdbcType = parseDataType(type);
        if (null == statement.getString(index)) {
            return null;
        }
        switch (jdbcType) {
            case BIT:
            case BOOLEAN:
                return statement.getBoolean(index);
            case TINYINT:
                return statement.getByte(index);
            case SMALLINT:
                return statement.getShort(index);
            case INTEGER:
                return statement.getInt(index);
            case BIGINT:
                return statement.getLong(index);
            case REAL:
            case FLOAT:
                return statement.getFloat(index);
            case DOUBLE:
                return statement.getDouble(index);
            case DECIMAL:
            case NUMERIC:
                return statement.getBigDecimal(index);
            case ROWID:
            case CHAR:
            case VARCHAR:
            case NVARCHAR:
            case NCHAR:
            case LONGNVARCHAR:
                return statement.getString(index);
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
                return statement.getBytes(index);
            case BLOB:
                return statement.getBlob(index);
            case CLOB:
                return statement.getClob(index);
            case DATE:
                return statement.getDate(index);
            case TIME:
                return statement.getTime(index);
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                return statement.getTimestamp(index);
            case REF:
                return statement.getObject(index);
            default:
                return statement.getString(index);
        }
    }

    public static Object getValueFromResultSet(ResultSet resultSet, int index, String type)
            throws SQLException {
        JDBCType jdbcType = parseDataType(type);
        switch (jdbcType) {
            case BIT:
            case BOOLEAN:
                return resultSet.getBoolean(index);
            case TINYINT:
                return resultSet.getByte(index);
            case SMALLINT:
                return resultSet.getShort(index);
            case INTEGER:
                return resultSet.getInt(index);
            case BIGINT:
                return resultSet.getLong(index);
            case REAL:
            case FLOAT:
                return resultSet.getFloat(index);
            case DOUBLE:
                return resultSet.getDouble(index);
            case DECIMAL:
            case NUMERIC:
                return resultSet.getBigDecimal(index);
            case ROWID:
            case CHAR:
            case VARCHAR:
            case NVARCHAR:
            case NCHAR:
            case LONGNVARCHAR:
                return resultSet.getString(index);
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
                return resultSet.getBytes(index);
            case BLOB:
                return resultSet.getBlob(index);
            case CLOB:
                return resultSet.getClob(index);
            case DATE:
                return resultSet.getDate(index);
            case TIME:
                return resultSet.getTime(index);
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                return resultSet.getTimestamp(index);
            default:
                return resultSet.getString(index);
        }
    }

    public static JDBCType parseDataType(String dataType) {
        if (StringUtils.isBlank(dataType)) {
            dataType = "varchar";
        }
        if (dataType.contains(OPEN_PARENTHESIS)) {
            // to get rid of dataType in mysql such as int(5) or number(10)
            dataType = dataType.substring(0, dataType.indexOf(OPEN_PARENTHESIS));
        }
        if ("BOOL".equalsIgnoreCase(dataType) || "BOOLEAN".equalsIgnoreCase(dataType)) {
            dataType = "BOOLEAN";
        } else if ("int".equalsIgnoreCase(dataType)
                || "PLS_INTEGER".equalsIgnoreCase(dataType)
                || "BINARY_INTEGER".equalsIgnoreCase(dataType)
                || "NATURAL".equalsIgnoreCase(dataType)
                || "NATURALN".equalsIgnoreCase(dataType)
                || "POSITIVE".equalsIgnoreCase(dataType)
                || "POSITIVEN".equalsIgnoreCase(dataType)
                || "SIGNTYPE".equalsIgnoreCase(dataType)
                || "SIMPLE_INTEGER".equalsIgnoreCase(dataType)
                || "MEDIUMINT".equalsIgnoreCase(dataType)
                || "INTEGER".equalsIgnoreCase(dataType)) {
            dataType = "INTEGER";
        } else if ("number".equalsIgnoreCase(dataType) || "numeric".equalsIgnoreCase(dataType)) {
            dataType = "numeric";
        } else if ("binary_double".equalsIgnoreCase(dataType) || "simple_double".equalsIgnoreCase(dataType)
                || "double".equalsIgnoreCase(dataType)) {
            dataType = "double";
        } else if ("binary_float".equalsIgnoreCase(dataType) || "simple_float".equalsIgnoreCase(dataType)
                || "float".equalsIgnoreCase(dataType)) {
            dataType = "float";
        } else if ("urowid".equalsIgnoreCase(dataType) || "rowid".equalsIgnoreCase(dataType)) {
            dataType = "rowid";
        } else if ("character".equalsIgnoreCase(dataType) || "varchar2".equalsIgnoreCase(dataType)
                || "varchar".equalsIgnoreCase(dataType)) {
            dataType = "varchar";
        } else if ("nvarchar2".equalsIgnoreCase(dataType) || "nvarchar".equalsIgnoreCase(dataType)) {
            dataType = "nvarchar";
        } else if ("datetime".equalsIgnoreCase(dataType) || "date".equalsIgnoreCase(dataType)) {
            dataType = "date";
        } else if ("sys_refcursor".equalsIgnoreCase(dataType)) {
            dataType = "ref";
        }

        JDBCType type;
        try {
            type = JDBCType.valueOf(dataType.toUpperCase());
        } catch (Exception e) {
            type = JDBCType.OTHER;
        }
        return type;
    }

    private static boolean validateValueWithType(String type, String value) {
        JDBCType jdbcType = parseDataType(type);
        try {
            if (Objects.isNull(value)) {
                return true;
            }
            switch (jdbcType) {
                case BIT:
                case BOOLEAN:
                    // string with lower or upper case of "true" will be parsed into true
                    // "1" will be parsed into true
                    // others will be treated as false
                    return true;
                case TINYINT:
                    Byte.parseByte(value);
                    return true;
                case SMALLINT:
                    Short.parseShort(value);
                    return true;
                case INTEGER:
                    Integer.parseInt(value);
                    return true;
                case BIGINT:
                    Long.parseLong(value);
                    return true;
                case REAL:
                case FLOAT:
                    Float.parseFloat(value);
                    return true;
                case DOUBLE:
                    Double.parseDouble(value);
                    return true;
                case DECIMAL:
                case NUMERIC:
                    new BigDecimal(value);
                    return true;
                case ROWID:
                case CHAR:
                case VARCHAR:
                case NVARCHAR:
                case NCHAR:
                case LONGNVARCHAR:
                case BINARY:
                case VARBINARY:
                case LONGVARBINARY:
                case BLOB:
                case CLOB:
                    return true;
                case DATE:
                    SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat(value));
                    sdf.parse(value);
                    return true;
                case TIME:
                    Time.valueOf(value);
                    return true;
                case TIMESTAMP:
                case TIMESTAMP_WITH_TIMEZONE:
                    Timestamp.valueOf(value);
                    return true;
                default:
                    return true;
            }
        } catch (Exception e) {
            log.warn("Value {} cannot be parsed into {}", value, type, e);
            return false;
        }
    }

    private static String getDateFormat(String value) {
        if (DATE_PATTERN.matcher(value).matches()) {
            return "yyyy-MM-dd";
        } else if (DATE_TIME_PATTERN.matcher(value).matches()) {
            return "yyyy-MM-dd HH:mm:ss";
        } else {
            throw new BadArgumentException(ErrorCodes.IllegalArgument, null,
                    String.format("Param value={%s} and param type={date} is not matched", value));
        }
    }
}
