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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.YamlUtils;

/**
 * {@link MySQLObjectNameUsingReservedWords}
 *
 * @author yh263208
 * @date 2024-03-05 17:13
 * @since ODC_release_4.2.4
 */
public class MySQLObjectNameUsingReservedWords extends BaseObjectNameUsingReservedWords {

    private static final String RESERVED_WORDS_FILE = "sql-check/obmysql-reserved-words.yml";
    private final static Set<String> RESERVED_WORDS;

    static {
        InputStream input = MySQLObjectNameUsingReservedWords.class
                .getClassLoader().getResourceAsStream(RESERVED_WORDS_FILE);
        Map<String, List<String>> type2ReservedWords = null;
        if (input != null) {
            try {
                String content = IOUtils.toString(input, StandardCharsets.UTF_8);
                type2ReservedWords = YamlUtils.from(content, new TypeReference<Map<String, List<String>>>() {});
            } catch (IOException e) {
                // eat exception
            }
        }
        if (type2ReservedWords == null) {
            RESERVED_WORDS = new HashSet<>();
        } else {
            RESERVED_WORDS = type2ReservedWords.values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toSet());
        }
    }

    @Override
    protected boolean isReservedWords(String objectName) {
        if (StringUtils.isEmpty(objectName)) {
            return false;
        }
        return RESERVED_WORDS.contains(StringUtils.unquoteMySqlIdentifier(objectName).toLowerCase());
    }

}
