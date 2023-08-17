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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * The {@link JdbcColumnMapper} is used to map the data returned by {@link ResultSet} to the
 * required data format
 *
 * @author yh263208
 * @date 2021-11-14 16:10
 * @since ODC_release_3.2.2
 */
public interface JdbcColumnMapper {
    /**
     * Map function, from {@link ResultSet} to {@link Object}
     *
     * @param data {@link CellData}
     * @return Mapped object
     */
    Object mapCell(@NonNull CellData data) throws SQLException, IOException;

    /**
     * If this {@link JdbcColumnMapper} supports the data type
     *
     * @param dataType data type value
     * @return supports flag
     */
    boolean supports(@NonNull DataType dataType);

}

