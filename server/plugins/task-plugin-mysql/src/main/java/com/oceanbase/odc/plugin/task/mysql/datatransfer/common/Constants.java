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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.common;

public class Constants {

    public static final String TXT_FILE_WRITER = "txtfilewriter";

    public static final String TXT_FILE_READER = "txtfilereader";

    public static final String MYSQL_READER = "mysqlreader";

    public static final String MYSQL_WRITER = "mysqlwriter";

    public static final String GROOVY_TRANSFORMER = "dx_groovy";

    public static final String DDL_SUFFIX = "-schema.sql";

    public static final String DISABLE_FK = "SET FOREIGN_KEY_CHECKS=0;";

    public static final String ENABLE_FK = "SET FOREIGN_KEY_CHECKS=1;";

    public static final String LINE_BREAKER = "\n";

    public static final String DROP_OBJECT_FORMAT = "DROP %s if exists %s;" + LINE_BREAKER;

    public static final String TRUNCATE_FORMAT = "TRUNCATE TABLE %s.%s;" + LINE_BREAKER;

    public static final String DEFAULT_PL_DELIMITER = "$$" + LINE_BREAKER;

    public static final String PL_DELIMITER_STMT = "DELIMITER " + DEFAULT_PL_DELIMITER;

    public static final String COMMIT_STMT = "commit";

    public static final int DEFAULT_BATCH_SIZE = 200;

    public static final String[] DEPENDENCIES =
            {"SEQUENCE", "TABLE", "VIEW", "FUNCTION", "PROCEDURE", "TRIGGER", "FILE"};

    public static final String[] DEFAULT_DATAX_JVM_PARAMS =
            {"-Xms1g", "-Xmx1g", "-XX:+HeapDumpOnOutOfMemoryError"};

}
