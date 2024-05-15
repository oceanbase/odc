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
package com.oceanbase.odc.service.schedule.model;

/**
 * @Author：tinker
 * @Date: 2022/11/18 17:18
 * @Descripition:
 */
public enum JobType {

    SQL_PLAN,
    PARTITION_PLAN,

    DATA_ARCHIVE,

    DATA_ARCHIVE_DELETE,
    DATA_DELETE,

    DATA_ARCHIVE_ROLLBACK,

    ONLINE_SCHEMA_CHANGE_COMPLETE;

    public boolean executeInTaskFramework() {
        return this == DATA_ARCHIVE || this == DATA_ARCHIVE_DELETE || this == DATA_DELETE
                || this == DATA_ARCHIVE_ROLLBACK;
    }

    public boolean isSync() {
        return this == DATA_ARCHIVE || this == DATA_ARCHIVE_DELETE || this == DATA_DELETE
                || this == DATA_ARCHIVE_ROLLBACK;
    }
}
