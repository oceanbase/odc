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
package com.oceanbase.odc.service.connection.util;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.jdbc.UrlParser;
import com.oceanbase.jdbc.util.Options;
import com.oceanbase.odc.service.connection.model.HostAddress;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DefaultJdbcUrlParser}
 *
 * @author yh263208
 * @date 2022-09-29 17:01
 * @since ODC_release_3.5.0
 * @see JdbcUrlParser
 */
@Slf4j
public class DefaultJdbcUrlParser implements JdbcUrlParser {

    private final UrlParser urlParser;

    public DefaultJdbcUrlParser(@NonNull String jdbcUrl) throws SQLException {
        this.urlParser = UrlParser.parse(jdbcUrl);
        if (this.urlParser == null) {
            throw new IllegalArgumentException("Invalid jdbc url: " + jdbcUrl);
        }
    }

    @Override
    public List<HostAddress> getHostAddresses() {
        return urlParser.getHostAddresses().stream().map(HostAddress::new).collect(Collectors.toList());
    }

    @Override
    public String getSchema() {
        return urlParser.getDatabase();
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        Options options = urlParser.getOptions();
        if (options == null) {
            return parameters;
        }
        Field[] fields = Options.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(options);
                if (value == null) {
                    continue;
                }
                parameters.putIfAbsent(field.getName(), value);
            } catch (Exception e) {
                log.warn("Failed to get object value", e);
            }
        }
        return parameters;
    }

}
