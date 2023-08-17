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
package com.oceanbase.odc.core.shared.constant;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;

/**
 * @author wenniu.ly
 * @date 2021/3/15
 */
public enum TaskType implements Translatable {
    /**
     * 异步任务
     */
    ASYNC,
    /**
     * 导入任务
     */
    IMPORT,
    /**
     * 导出任务
     */
    EXPORT,
    /**
     * 模拟数据
     */
    MOCKDATA,
    /**
     * 回滚任务
     */
    ROLLBACK,
    /**
     * 权限申请
     */
    PERMISSION_APPLY,
    /**
     * 影子表同步任务
     */
    SHADOWTABLE_SYNC,
    /**
     * 分区计划
     */
    PARTITION_PLAN,
    /**
     * sql 检查任务
     */
    SQL_CHECK,
    /**
     * Schedule 变更任务
     */
    ALTER_SCHEDULE,
    /**
     * 生成备份回滚方案
     */
    GENERATE_ROLLBACK,
    /**
     * 无锁结构变更
     */
    ONLINE_SCHEMA_CHANGE,
    /**
     * 导出结果集
     */
    EXPORT_RESULT_SET,

    PRE_CHECK,
    ;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }

    public boolean needsPreCheck() {
        return this == ASYNC || this == ONLINE_SCHEMA_CHANGE || this == ALTER_SCHEDULE || this == EXPORT_RESULT_SET;
    }

    public boolean needForExecutionStrategy() {
        return !(this == PRE_CHECK || this == SQL_CHECK || this == GENERATE_ROLLBACK);
    }

}
