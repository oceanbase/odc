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
package com.oceanbase.tools.dbbrowser.schema;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;

import lombok.NonNull;

/**
 * {@link DBSchemaAccessorSqlMappers}
 *
 * @author yh263208
 * @date 2023-03-03 14:11
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class DBSchemaAccessorSqlMappers {

    private static final List<String> SQL_MAPPER_FILE_PATHS = new ArrayList<>();
    private static final Map<String, DBSchemaAccessorSqlMapper> VERSION_2_SQL_MAPPERS = new HashMap<>();

    static {
        SQL_MAPPER_FILE_PATHS.addAll(Arrays.asList(
                StatementsFiles.OBMYSQL_40X,
                StatementsFiles.OBMYSQL_3X,
                StatementsFiles.OBMYSQL_2276,
                StatementsFiles.OBMYSQL_225X,
                StatementsFiles.OBMYSQL_1479,
                StatementsFiles.MYSQL_5_7_40,
                StatementsFiles.OBORACLE_3_x,
                StatementsFiles.OBORACLE_4_0_x,
                StatementsFiles.ORACLE_11_g));
        for (String path : SQL_MAPPER_FILE_PATHS) {
            URL url = DBSchemaAccessorSqlMappers.class.getClassLoader().getResource(path);
            if (url == null) {
                throw new IllegalStateException("Failed to get url by path, " + path);
            }
            DBSchemaAccessorSqlMapper sqlMappers = fromYaml(url, DBSchemaAccessorSqlMapper.class);
            VERSION_2_SQL_MAPPERS.putIfAbsent(path, sqlMappers);
        }
    }

    public static DBSchemaAccessorSqlMapper get(@NonNull String key) {
        DBSchemaAccessorSqlMapper mapper = VERSION_2_SQL_MAPPERS.get(key);
        if (mapper == null) {
            throw new NullPointerException("Failed to get mapper by " + key);
        }
        return mapper;
    }

    private static <T> T fromYaml(URL url, Class<T> classType) {
        if (url == null) {
            return null;
        }
        try {
            return yamlMapper().readValue(url, classType);
        } catch (IOException ex) {
            return null;
        }
    }

    private static ObjectMapper yamlMapper() {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return yamlMapper;
    }

}
