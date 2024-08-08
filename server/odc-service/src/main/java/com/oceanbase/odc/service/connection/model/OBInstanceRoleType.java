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
package com.oceanbase.odc.service.connection.model;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oceanbase.odc.common.i18n.Translatable;

import lombok.Getter;

public enum OBInstanceRoleType implements Translatable {
    // not a primary/standby instance
    // 普通实例，不是主实例/从实例
    NORMAL("NORMAL"),

    // 主实例
    PRIMARY("PRIMARY"),

    // 物理从实例
    PHYSICAL_STANDBY("PHYSICAL_STANDBY");

    @Getter
    private String value;

    OBInstanceRoleType(String value) {
        this.value = value;
    }

    /**
     * 将枚举类型转换为字符串
     *
     * @return 返回枚举类型的名称
     */
    @JsonValue
    public String getName() {
        return this.name();
    }

    /**
     * 根据字符串值获取枚举类型
     *
     * @param value 枚举类型的字符串值
     * @return 返回对应的枚举类型
     */
    @JsonCreator
    public static OBInstanceRoleType fromValue(String value) {
        for (OBInstanceRoleType roleType : OBInstanceRoleType.values()) {
            if (StringUtils.equalsIgnoreCase(roleType.value, value)) {
                return roleType;
            }
        }
        return OBInstanceRoleType.NORMAL;
    }

    /**
     * 获取枚举类型的编码
     *
     * @return 返回枚举类型的名称
     */
    @Override
    public String code() {
        return name();
    }
}
