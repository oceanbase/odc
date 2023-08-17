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
package com.oceanbase.odc.service.config.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Pattern.Flag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.config.util.ConfigMetaInfo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: Lebie
 * @Date: 2021/7/23 上午11:00
 * @Description: []
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Component
public class OrganizationConfig {
    /**
     * Default delimiter
     */
    @NotBlank(message = "Delimiter for organization config can not be blank")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Delimiter for sql-execute")
    @Value("${sqlexecute.defaultDelimiter:;}")
    private String defaultDelimiter;
    /**
     * Auto commit setting for mysql mode eg. ON/OFF
     */
    @NotBlank(message = "Mysql auto commit mode can not be blank")
    @Pattern(regexp = "ON|OFF", flags = Flag.CASE_INSENSITIVE,
            message = "Mysql auto commit mode can only accept the value ON/OFF")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Auto commit flag for OB-Mysql mode")
    @Value("${sqlexecute.mysqlAutoCommitMode:ON}")
    private String mysqlAutoCommitMode;
    /**
     * Auto commit setting for oracle mode eg. ON/OFF
     */
    @NotBlank(message = "Oracle auto commit mode can not be blank")
    @Pattern(regexp = "ON|OFF", flags = Flag.CASE_INSENSITIVE,
            message = "Oracle auto commit mode can only accept the value ON/OFF")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Auto commit flag for OB-Oracle mode")
    @Value("${sqlexecute.oracleAutoCommitMode:ON}")
    private String oracleAutoCommitMode;
    /**
     * Query limit
     */
    @Min(value = 0, message = "Query limit can not be negative")
    @Max(value = Integer.MAX_VALUE, message = "Query limit can not be bigger than " + Integer.MAX_VALUE)
    @NotNull(message = "Query limit can not be null")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Count limit for resultSet")
    @Value("${sqlexecute.defaultQueryLimit:1000}")
    private Integer defaultQueryLimit;

    @NotBlank(message = "Default object dragging option can not be blank")
    @Pattern(regexp = "object_name|select_stmt|insert_stmt|update_stmt|delete_stmt", flags = Flag.CASE_INSENSITIVE,
            message = "Default object dragging option can only accept the value object_name|select_stmt|insert_stmt|update_stmt|delete_stmt")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Default object dragging option")
    @Value("${sqlexecute.defaultObjectDraggingOption:object_name}")
    private String defaultObjectDraggingOption;

    /**
     * Session Mode, candidate modes are as follow:
     *
     * <pre>
     *     1. MultiSession
     *     2. SingleSession
     * </pre>
     */
    @NotBlank(message = "Session mode can not be blank")
    @Pattern(regexp = "SingleSession|MultiSession", flags = Flag.CASE_INSENSITIVE,
            message = "Session mode can only accept the value SingleSession|MultiSession")
    @ConfigMetaInfo(prefix = "connect", description = "Session mode for connection")
    @Value("${connect.sessionMode:MultiSession}")
    private String sessionMode;

    @NotBlank(message = "Sql check mode can not be blank")
    @Pattern(regexp = "AUTO|MANUAL", flags = Flag.CASE_INSENSITIVE,
            message = "Sql check mode can only accept the value AUTO|MANUAL")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Check mode for sqlexecute")
    @Value("${sqlexecute.sqlCheckMode:AUTO}")
    private String sqlCheckMode;

}

