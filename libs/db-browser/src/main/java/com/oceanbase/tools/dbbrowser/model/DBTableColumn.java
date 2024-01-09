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
package com.oceanbase.tools.dbbrowser.model;

import java.util.List;

import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Table column metadata, mapping to <br>
 * - Oracle: ALL_TAB_COLUMNS <br>
 * - MySQL: INFORMATION_SCHEMA.COLUMNS <br>
 *
 *
 * Special consideration: <br>
 * 1. About column order while alter table, e.g. alter table add column xxx first|before xxx|after
 * xxx，no related fields defined here, client may use full columns list to identify columns order.
 * 2. About column renaming scenario, regard as renaming while ordinalPosition!=null for new
 * columnName.
 */
@Data
@EqualsAndHashCode(exclude = {"name", "warning", "schemaName", "tableName", "ordinalPosition", "keyType"})
public class DBTableColumn implements DBObject, DBObjectWarningDescriptor {
    /**
     * 所属 schemaName
     */
    private String schemaName;

    /**
     * 所属 tableName
     */
    private String tableName;

    private String name;

    /**
     * The typeName value is the type name only with no other information
     */
    private String typeName;

    /**
     * The fullTypeName value contains the type name and possibly other information such as the
     * precision or length.
     */
    private String fullTypeName;


    private Integer scale;

    /**
     * <pre>
     * - MySQL NUMBER: NUMBERIC_PRECISION
     * - MySQL DATETIME: DATETIME_PRECISION
     * </pre>
     */
    private Long precision;

    /**
     * Oracle special, for datatype TIMESTAMP, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH LOCAL TIME ZONE,
     * INTERVAL DAY TO SECOND
     */
    private Integer secondPrecision;

    /**
     * Oracle special, for datatype INTERVAL DAY TO SECOND
     */
    private Integer dayPrecision;

    /**
     * Oracle special, for datatype INTERVAL YEAR TO MONTH
     */
    private Integer yearPrecision;

    /**
     * MySQL: unsigned, zerofill Oracle: REF or POINTER
     */
    private List<String> typeModifiers;

    /**
     * required if nullable=false
     */
    private Boolean nullable;


    private String defaultValue;

    private Boolean virtual;

    private String comment;

    private Integer ordinalPosition;

    /**
     * For string columns, the maximum length in characters; for non-char type, the precision
     */
    private Long maxLength;

    /**
     * MySQL special
     */
    private String charsetName;

    /**
     * MySQL special
     */
    private String collationName;

    /**
     * MySQL special
     */
    private String genExpression;

    /**
     * MySQL special
     */
    private Boolean autoIncrement;

    /**
     * MySQL special
     */
    private Boolean unsigned;

    /**
     * MySQL special
     */
    private Boolean zerofill;

    /**
     * MySQL special, for Enum data type
     */
    private List<String> enumValues;

    /**
     * MySQL special, if stored for virtual column
     */
    private Boolean stored;

    /**
     * MySQL special, ON UPDATE CURRENT_TIMESTAMP
     */
    private Boolean onUpdateCurrentTimestamp;

    /**
     * MySQL special
     */
    private String extraInfo;

    /**
     * Oracle special, BYTE | CHAR. B indicates that the column uses BYTE length semantics. C indicates
     * that the column uses CHAR length semantics. NULL indicates the datatype is not any of the
     * following:CHAR, VARCHAR2, NCHAR, NVARCHAR2
     */
    private CharUnit charUsed;

    /**
     * Oracle special
     */
    private Boolean hidden;

    private String warning;


    private KeyType keyType;


    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.COLUMN;
    }

    public enum KeyType {
        /**
         * If COLUMN_KEY is PRI, the column is a PRIMARY KEY or is one of the columns in a multiple-column
         * PRIMARY KEY.
         */
        PRI,

        /**
         * If COLUMN_KEY is UNI, the column is the first column of a UNIQUE index. (A UNIQUE index permits
         * multiple NULL values, but you can tell whether the column permits NULL by checking the Null
         * column.)
         */
        UNI,

        /**
         * If COLUMN_KEY is MUL, the column is the first column of a nonunique index in which multiple
         * occurrences of a given value are permitted within the column.
         */
        MUL
    }

    public enum CharUnit {
        BYTE("B"),
        CHAR("C");

        private String text;

        CharUnit(String text) {
            this.text = text;
        }

        public static CharUnit fromString(String text) {
            for (CharUnit unit : CharUnit.values()) {
                if (unit.text.equalsIgnoreCase(text)) {
                    return unit;
                }
            }
            return null;
        }
    }

    public void fillDefaultValue(String defaultValue) {
        if (MySQLConstants.CURRENT_TIMESTAMP.equalsIgnoreCase(defaultValue)
                && !DataTypeUtil.isStringType(this.typeName)) {
            defaultValue = (defaultValue + "(" + precision + ")");
        }
        this.defaultValue = defaultValue;
    }

}
