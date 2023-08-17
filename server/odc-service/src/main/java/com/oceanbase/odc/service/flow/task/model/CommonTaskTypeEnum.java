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
package com.oceanbase.odc.service.flow.task.model;

/**
 * Created by mogao.zj
 */
public enum CommonTaskTypeEnum {
    /**
     * 未知任务类型
     */
    UNKNOWN("UNKNOWN", "未知任务类型"),
    /**
     * 执行sql进行审批管理任务类型
     */
    INTERCEPT_EXECUTED_SQL("INTERCEPT_EXECUTED_SQL", "执行sql进行审批管理"),
    /**
     * 模拟数据任务类型
     */
    MOCK_DATA("MOCK", "模拟数据任务类型");

    private String name = "";
    private String desc;

    CommonTaskTypeEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public static CommonTaskTypeEnum getEnumByName(String name) {
        CommonTaskTypeEnum[] types = CommonTaskTypeEnum.values();
        for (CommonTaskTypeEnum type : types) {
            if (name.equalsIgnoreCase(type.name)) {
                return type;
            }
        }

        return CommonTaskTypeEnum.UNKNOWN;
    }
}
