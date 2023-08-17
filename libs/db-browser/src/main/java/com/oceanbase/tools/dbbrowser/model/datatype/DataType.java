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

/**
 * {@link DataType}
 *
 * @author yh263208
 * @date 2022-06-14 11:20
 * @since ODC_release_3.4.0
 */
public interface DataType {
    /**
     * Gets the designated column's number of digits to right of the decimal point. null is returned for
     * data types where the scale is not applicable.
     *
     * @return scale
     */
    Integer getScale();

    /**
     * Get the designated column's specified column size.
     * 
     * <pre>
     *     For numeric data, this is the maximum precision.
     *     For character data, this is the length in characters.
     *     For datetime datatypes, this is the length in characters of the String representation
     *     (assuming the * maximum allowed precision of the fractional seconds component).
     *     For binary data, this is the length in bytes.
     *     For the ROWID datatype, this is the length in bytes. null is returned for data types where the
     *     column size is not applicable.
     * </pre>
     *
     * @return precision
     */
    Integer getPrecision();

    /**
     * Get data name. eg. timestamp(6) with local time zone -> timestamp with local time zone
     *
     * @return name value
     */
    String getDataTypeName();

}
