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
package com.oceanbase.odc.core.session;

import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;

/**
 * Some constants related to connection
 *
 * @author yh263208
 * @date 2021-11-04 11:26
 * @since ODC_release_3.2.2
 */
public class ConnectionSessionConstants {
    /**
     * Connection session expiration time
     */
    public static final long SESSION_EXPIRATION_TIME_SECONDS = 3600 * 8L;
    /**
     * Default setting's value for {@link ConnectionSessionManager} scan interval
     */
    public static final long SESSION_MANAGER_SCAN_INTERVAL_MILLIS = 10000;
    /**
     * Need to store userid in the database session, this field is the stored key
     */
    public static final String USER_ID_KEY = "CREATOR_ID";
    /**
     * The default schema of the current database session needs to be stored in the database session in
     * the form of attributes, this is the key
     */
    public static final String CURRENT_SCHEMA_KEY = "CURRENT_SCHEMA";
    public static final String CONNECT_SCHEMA_KEY = "CONNECT_SCHEMA";
    /**
     * OceanBase special, tenant name
     */
    public static final String TENANT_NAME = "TENANT_NAME";


    /**
     * OceanBase special, cluster name
     */
    public static final String CLUSTER_NAME = "CLUSTER_NAME";

    /**
     * The query limit of the current database session needs to be stored in the database session in the
     * form of attributes, this is the key
     */
    public static final String QUERY_LIMIT_KEY = "QUERY_LIMIT";
    /**
     * The sql processor of the current database session needs to be stored in the database session in
     * the form of attributes, this is the key
     */
    public static final String SQL_COMMENT_PROCESSOR_KEY = "SQL_COMMENT_PROCESSOR";
    /**
     * The connection config current database session needs to be stored in the database session in the
     * form of attributes, this is the key
     */
    public static final String CONNECTION_CONFIG_KEY = "CONNECTION_CONFIG";
    /**
     * The connection config current database session needs to be stored in the database session in the
     * form of attributes, this is the key
     */
    public static final String OB_VERSION = "OB_VERSION";
    /**
     * The connection account type current database session needs to be stored in the database session
     * in the form of attributes, this is the key
     */
    public static final String QUERY_CACHE_KEY = "QUERY_CACHE";
    public static final String FUTURE_JDBC_RESULT_KEY = "FUTURE_JDBC_RESULT";
    public static final String ASYNC_EXECUTE_CONTEXT_KEY = "ASYNC_EXECUTE_CONTEXT";
    /**
     * The connection_id current database session needs to be stored in the database session in the form
     * of attributes, this is the key
     */
    public static final String CONNECTION_ID_KEY = "CONNECTION_ID";
    public static final String OB_PROXY_SESSID_KEY = "PROXY_SESSID";
    /**
     * The {@link BinaryDataManager} session needs to be stored in the database session in the form of
     * attributes, this is the key
     */
    public static final String BINARY_FILE_MANAGER_KEY = "BINARY_FILE_MANAGER";
    /**
     * Working dir name for {@link BinaryDataManager}
     */
    public static final String SESSION_DATABINARY_DIR_NAME = "SESSION_DATABINARY_DIR";
    /**
     * The database session may have a file upload requirement, and the uploaded file is placed in this
     * subdirectory
     */
    public static final String SESSION_UPLOAD_DIR_NAME = "SESSION_UPLOAD_DIR";
    public static final String SESSION_TIME_ZONE = "TIME_ZONE";
    public static final String OB_ARCHITECTURE = "OB_ARCHITECTURE";
    public static final String CONSOLE_DS_KEY = "CONSOLE-DATASOURCE";
    public static final String SYS_DS_KEY = "SYS-DATASOURCE";
    public static final String BACKEND_DS_KEY = "BACKEND-DATASOURCE";
    public static final String CONNECTION_RESET_KEY = "CONSOLE_SESSION_RESET";
    public static final String CONNECTION_KILLQUERY_KEY = "CONNECTION_KILLQUERY_KEY";

    /**
     * Column meta info accessor stored in connectionSession
     */
    public static final String COLUMN_ACCESSOR_KEY = "DATASOURCE_COLUMN_ACCESSOR";
    /**
     * BinaryContentMetaData cache stored in connectionSession
     */
    public static String BINARY_CONTENT_METADATA_CACHE = "BINARY_CONTENT_METADATA_CACHE";
    public static String RULE_SET_ID_NAME = "RULE_SET_ID";
    public static String NLS_DATE_FORMAT_NAME = "NLS_DATE_FORMAT";
    public static String NLS_TIMESTAMP_FORMAT_NAME = "NLS_TIMESTAMP_FORMAT";
    public static String NLS_TIMESTAMP_TZ_FORMAT_NAME = "NLS_TIMESTAMP_TZ_FORMAT";

}
