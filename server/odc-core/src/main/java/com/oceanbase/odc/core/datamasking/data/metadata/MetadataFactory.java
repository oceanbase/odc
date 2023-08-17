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
package com.oceanbase.odc.core.datamasking.data.metadata;

import java.sql.JDBCType;

import org.apache.commons.lang3.StringUtils;

/**
 * @author wenniu.ly
 * @date 2022/8/25
 */
public class MetadataFactory {
    public static final String OPEN_PARENTHESIS = "(";

    public static Metadata createMetadata(String fieldName, String type) {
        JDBCType jdbcType = parseDataType(type);
        switch (jdbcType) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
                return new IntegerMetadata().withFieldName(fieldName).withType(type);
            case BIGINT:
                return new LongMetadata().withFieldName(fieldName).withType(type);
            case REAL:
            case FLOAT:
                return new FloatMetadata().withFieldName(fieldName).withType(type);
            case DOUBLE:
            case DECIMAL:
            case NUMERIC:
                return new DoubleMetadata().withFieldName(fieldName).withType(type);
            case CHAR:
            case NCHAR:
                return new FixedLengthStringMetadata().withFieldName(fieldName).withType(type);
            case BIT:
            case BOOLEAN:
            case ROWID:
            case VARCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
            case CLOB:
            case DATE:
            case TIME:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
            default:
                return new StringMetadata().withFieldName(fieldName).withType(type);
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
        if ("BOOL".equalsIgnoreCase(dataType)) {
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
                || "MEDIUMINT".equalsIgnoreCase(dataType)) {
            dataType = "INTEGER";
        } else if ("number".equalsIgnoreCase(dataType)) {
            dataType = "numeric";
        } else if ("binary_double".equalsIgnoreCase(dataType) || "simple_double".equalsIgnoreCase(dataType)) {
            dataType = "double";
        } else if ("binary_float".equalsIgnoreCase(dataType) || "simple_float".equalsIgnoreCase(dataType)) {
            dataType = "float";
        } else if ("urowid".equalsIgnoreCase(dataType)) {
            dataType = "rowid";
        } else if ("character".equalsIgnoreCase(dataType) || "varchar2".equalsIgnoreCase(dataType)) {
            dataType = "varchar";
        } else if ("nvarchar2".equalsIgnoreCase(dataType)) {
            dataType = "nvarchar";
        } else if ("datetime".equalsIgnoreCase(dataType)) {
            dataType = "date";
        }

        JDBCType type;
        try {
            type = JDBCType.valueOf(dataType.toUpperCase());
        } catch (Exception e) {
            // if type cannot be recognized, use varchar as default
            type = JDBCType.VARCHAR;
        }
        return type;
    }
}
