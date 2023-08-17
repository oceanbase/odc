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
package com.oceanbase.tools.dbbrowser.model.datatype.parser;

/**
 * {@link DataTypeToken}
 *
 * @author yh263208
 * @date 2022-06-27 10:57
 * @since ODC_release_3.4.0
 */
public interface DataTypeToken {
    int INVALID_TYPE = 0;
    int NAME_TYPE = 1;
    int BRACKETS_TYPE = 2;
    int NUMBER_TYPE = 3;

    /**
     * Token text value
     *
     * @return text value
     */
    String getText();

    /**
     * start index
     *
     * @return index value
     */
    int getStartIndex();

    /**
     * stop index of the token
     *
     * @return stop index value
     */
    int getStopIndex();

    /**
     * index type
     *
     * @return type value
     */
    int getType();

}
