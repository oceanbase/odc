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
 * {@link CharDataType}
 *
 * @author yh263208
 * @date 2022-06-14 11:54
 * @since ODC_release_3.4.0
 */
public interface CharDataType extends DataType {
    String CHAR_TYPE = "CHAR";
    String BYTE_TYPE = "BYTE";

    /**
     * candidate value:
     * 
     * <pre>
     *     B: BYTE indicates that the column will have byte length semantics;
     *     C: CHAR indicates that the column will have character semantics.
     * </pre>
     *
     * @return char used value
     */
    String getCharUsed();

}
