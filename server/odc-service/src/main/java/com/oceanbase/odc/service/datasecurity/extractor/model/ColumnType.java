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
package com.oceanbase.odc.service.datasecurity.extractor.model;

/**
 * @author gaoda.xy
 * @date 2023/6/5 19:23
 */
public enum ColumnType {
    /**
     * Physical column (Column that actually exist in the database system)
     */
    PHYSICAL,

    /**
     * Temporary columns inherited directly from the underlying temporary table
     */
    INHERITANCE,

    /**
     * Temporary column formed by the underlying temporary table through computation
     */
    COMPUTATION,

    /**
     * Temporary column formed by the underlying temporary table through function call
     */
    FUNCTION_CALL,

    /**
     * Temporary column formed by the underlying temporary table through case when
     */
    CASE_WHEN,

    /**
     * Temporary column formed by the select body
     */
    SELECT,

    /**
     * Temporary column formed by the constant
     */
    CONSTANT,

    /**
     * Temporary column formed by the underlying temporary table through join operation
     */
    JOIN,

    /**
     * Temporary column formed by the underlying temporary table through union operation
     */
    UNION;

    public boolean isTemporary() {
        return this != PHYSICAL;
    }
}
