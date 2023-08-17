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
package com.oceanbase.odc.service.datatransfer.loader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.oceanbase.odc.service.datatransfer.loader.spliter.SqlSplitterForThirdParty;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlSqlSingleFileOutput extends AbstractThirdPartyOutput {
    private static final char[] BLANK_CHARACTERS = new char[] {'\n', '\r', ' ', '\t'};
    private static final char DEFAULT_PL_DELIMITER = '/';
    private static final char DEFAULT_SQL_DELIMITER = ';';
    private static final String DELIMITER_NAME = "delimiter";
    private static final char BLANK_SPACE = ' ';
    private static final char LINE_BREAK = '\n';

    public PlSqlSingleFileOutput(File origin) {
        super(origin);
    }

    @Override
    public boolean supports() {
        if (!origin.getName().endsWith(".sql")) {
            return false;
        }
        try (FileReader fileReader = new FileReader(origin);
                BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            for (int num = 0; num < 10 && (line = reader.readLine()) != null; num++) {
                if (line.startsWith("prompt") || line.startsWith("PROMPT")) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse file, will use origin file.", e);
        }
        return false;
    }

    @Override
    public void toObLoaderDumperCompatibleFormat(File dest) throws Exception {
        List<String> sqls = new SqlSplitterForThirdParty(true, true, true).split(origin);
        try (FileWriter fileWriter = new FileWriter(dest);
                BufferedWriter writer = new BufferedWriter(fileWriter)) {
            char delimiter = DEFAULT_SQL_DELIMITER;

            for (String sql : sqls) {
                char currentDelimiter = getDelimiter(sql);
                if (currentDelimiter != DEFAULT_PL_DELIMITER && currentDelimiter != DEFAULT_SQL_DELIMITER) {
                    sql += DEFAULT_SQL_DELIMITER;
                    currentDelimiter = DEFAULT_SQL_DELIMITER;
                }
                if (currentDelimiter != delimiter) {
                    delimiter = currentDelimiter;
                    sql = DELIMITER_NAME + BLANK_SPACE + delimiter + LINE_BREAK + sql;
                }
                writer.write(sql + LINE_BREAK + LINE_BREAK);
            }

            writer.flush();
        }
    }

    @Override
    public String getNewFilePrefix() {
        return "plsql";
    }

    private char getDelimiter(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (ArrayUtils.contains(BLANK_CHARACTERS, chars[i])) {
                continue;
            }
            return chars[i];
        }
        return DEFAULT_SQL_DELIMITER;
    }
}
