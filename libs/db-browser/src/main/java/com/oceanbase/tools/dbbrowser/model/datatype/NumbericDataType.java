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
 * {@link NumbericDataType} which has max value and min value such as number, float, etc.
 *
 * @author yh263208
 * @date 2022-06-6-14 12:05
 * @since ODC_release_3.4.0
 */
public interface NumbericDataType<T> extends DataType {
    /**
     * Max value with give {@link DataType#getPrecision()} and {@link DataType#getScale()}
     *
     * @return high value
     */
    T getHighValue();

    /**
     * Min value with give {@link DataType#getPrecision()} and {@link DataType#getScale()}
     *
     * @return low value
     */
    T getLowValue();

}
