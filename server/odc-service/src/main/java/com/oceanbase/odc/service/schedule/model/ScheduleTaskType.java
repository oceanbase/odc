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

import org.springframework.core.env.Environment;

import com.oceanbase.odc.service.common.util.SpringContextUtil;

/**
 * @Author：tinker
 * @Date: 2024/6/18 16:56
 * @Descripition:
 */
public enum ScheduleTaskType {

    SQL_PLAN,

    PARTITION_PLAN,

    DATA_ARCHIVE,

    DATA_ARCHIVE_DELETE,

    DATA_DELETE,

    DATA_ARCHIVE_ROLLBACK,

    ONLINE_SCHEMA_CHANGE_COMPLETE,

    LOGICAL_DATABASE_CHANGE,

    LOAD_DATA;

    public boolean isExecuteInTaskFramework() {
        switch (this) {
            case SQL_PLAN: {
                String property = SpringContextUtil.getBean(Environment.class).getProperty("odc.iam.auth.type");
                return "obcloud".equals(property);
            }
            case DATA_ARCHIVE:
            case DATA_ARCHIVE_DELETE:
            case DATA_DELETE:
            case DATA_ARCHIVE_ROLLBACK:
            case LOGICAL_DATABASE_CHANGE:
            case LOAD_DATA: {
                return true;
            }
            case ONLINE_SCHEMA_CHANGE_COMPLETE:
            case PARTITION_PLAN: {
                return false;
            }
            default:
                return false;
        }
    }

}
