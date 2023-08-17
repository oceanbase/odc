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
package com.oceanbase.odc.core.sql.execute.model;

/**
 * SQL execute status enum
 *
 * @author yh263208
 * @date 2021-11-01 22:06
 * @since ODC_release_3.2.2
 */
public enum SqlExecuteStatus {
    /**
     * An asynchronous execution sql has been created but has not been put into an asynchronous thread
     * for execution
     */
    CREATED,
    /**
     * sql is executing
     */
    RUNNING,
    /**
     * sql successfully executed asynchronously
     */
    SUCCESS,
    /**
     * sql asynchronous execution canceled
     */
    CANCELED,
    /**
     * sql asynchronous execution failed
     */
    FAILED

}

