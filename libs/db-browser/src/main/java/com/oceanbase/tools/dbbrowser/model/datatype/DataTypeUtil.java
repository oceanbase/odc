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
package com.oceanbase.tools.dbbrowser.model.datatype;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;

/**
 * {@link DataTypeUtil}
 *
 * @author yh263208
 * @date 2022-06-23 14:22
 * @since ODC_release_3.4.0
 */
public class DataTypeUtil {
    /**
     * Binary data type
     */
    private final static String[] BINARY_DATA_TYPES = new String[] {
            "blob",
            "clob",
            "raw",
            "longblob",
            "mediumblob",
            "tinyblob"
    };
    /**
     * raw data type
     */
    private final static String RAW_DATA_TYPES = "raw";
    private final static Pattern NUMBER_PATTERN = Pattern.compile("^[-\\+]?[\\d]*$");

    /**
     * String data type
     */
    private final static String[] STRING_DATA_TYPES = new String[] {
            "char",
            "varchar",
            "varchar2",
            "tinytext",
            "mediumtext",
            "longtext",
            "text",
            "NCHAR",
            "NVARCHAR",
            "NVARCHAR2",
            "CHARACTER"
    };

    /**
     * Numeric data type
     */
    private final static String[] INTEGER_DATA_TYPES = new String[] {
            "tinyint",
            "smallint",
            "mediumint",
            "int",
            "integer",
            "bigint"
    };

    /**
     * Date data type
     */
    private final static String[] DATE_DATA_TYPES = new String[] {
            "date",
            "time",
            "datetime",
            "timestamp",
            "year"
    };

    private final static String[] PL_BASIC_DATA_TYPES = new String[] {
            "BOOL",
            "BOOLEAN",
            "PLS_INTEGER",
            "BINARY_INTEGER",
            "NATURAL",
            "NATURALN",
            "POSITIVE",
            "POSITIVEN",
            "SIGNTYPE",
            "SIMPLE_INTEGER",
            "MEDIUMINT",
            "number",
            "binary_double",
            "simple_double",
            "binary_float",
            "simple_float",
            "urowid",
            "numeric",
            "double",
            "float",
            "rowid",
            "blob",
            "clob",
            "raw",
            "longblob",
            "mediumblob",
            "tinyblob",
            "char",
            "varchar",
            "varchar2",
            "tinytext",
            "mediumtext",
            "longtext",
            "text",
            "NCHAR",
            "NVARCHAR",
            "NVARCHAR2",
            "CHARACTER",
            "tinyint",
            "smallint",
            "mediumint",
            "int",
            "integer",
            "bigint",
            "date",
            "time",
            "datetime",
            "timestamp",
            "year",
            "raw"
    };

    /**
     * Test whether a data type is a large field type
     *
     * @param dataTypeName name of data type
     * @return test result
     */
    public static boolean isBinaryType(String dataTypeName) {
        for (String type : BINARY_DATA_TYPES) {
            if (type.equalsIgnoreCase(dataTypeName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStringType(String dataTypeName) {
        for (String type : STRING_DATA_TYPES) {
            if (type.equalsIgnoreCase(dataTypeName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIntegerType(String dataTypeName) {
        for (String type : INTEGER_DATA_TYPES) {
            if (type.equalsIgnoreCase(dataTypeName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDateType(String dataTypeName) {
        for (String type : DATE_DATA_TYPES) {
            if (type.equalsIgnoreCase(dataTypeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether a data type is a large field type
     *
     * @param dataTypeName name of data type
     * @return test result
     */
    public static boolean isRawType(String dataTypeName) {
        return RAW_DATA_TYPES.equalsIgnoreCase(dataTypeName);
    }

    public static boolean isBinaryType(DataType dataType) {
        return isBinaryType(dataType.getDataTypeName());
    }

    public static boolean isRawType(@NonNull DataType dataType) {
        return isRawType(dataType.getDataTypeName());
    }

    public static boolean isBitType(String dataTypeName) {
        return StringUtils.containsIgnoreCase(dataTypeName, "BIT");
    }

    public static boolean isBitType(@NonNull DataType dataType) {
        return isBitType(dataType.getDataTypeName());
    }

    public static boolean isDateType(@NonNull DataType dataType) {
        return StringUtils.containsIgnoreCase(dataType.getDataTypeName(), "DATE");
    }

    public static boolean isNumericValue(String value) {
        if (null == value) {
            return false;
        }
        return NUMBER_PATTERN.matcher(value).matches();
    }

    public static boolean isExtType(String dataType) {
        for (String type : PL_BASIC_DATA_TYPES) {
            if (type.equalsIgnoreCase(dataType)) {
                return false;
            }
        }
        return true;
    }

}
