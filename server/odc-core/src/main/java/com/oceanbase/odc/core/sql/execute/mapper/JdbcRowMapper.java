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
import java.util.List;

import lombok.NonNull;

/**
 * {@link JdbcRowMapper}
 *
 * @author yh263208
 * @date 2022-06-13 15:43
 * @since ODC_release_3.4.0
 */
public interface JdbcRowMapper {

    /**
     * Map function, from {@link ResultSet} to list of {@link Object}
     *
     * @param resultSet {@link ResultSet}
     * @return Mapped objects
     */
    List<Object> mapRow(@NonNull ResultSet resultSet) throws SQLException, IOException;

}
