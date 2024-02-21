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

import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.service.config.util.ConfigMetaInfo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Packaged object which contained by application
 *
 * @author yh263208
 * @date 2021-05-19 20:00
 * @since ODC_release_2.4.2
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UserConfig {
    /**
     * Delimiter, default is ;
     */
    @NotBlank(message = "Delimiter for user config can not be blank")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Delimiter for sql-execute")
    private String defaultDelimiter = ";";
    /**
     * Auto commit setting for mysql mode eg. ON/OFF
     */
    @NotBlank(message = "Mysql auto commit mode can not be blank")
    @Pattern(regexp = "ON|OFF", flags = Flag.CASE_INSENSITIVE,
            message = "Mysql auto commit mode can only accept the value ON/OFF")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Auto commit flag for OB-Mysql mode")
    private String mysqlAutoCommitMode = "ON";
    /**
     * Auto commit setting for oracle mode eg. ON/OFF
     */
    @NotBlank(message = "Oracle auto commit mode can not be blank")
    @Pattern(regexp = "ON|OFF", flags = Flag.CASE_INSENSITIVE,
            message = "Oracle auto commit mode can only accept the value ON/OFF")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Auto commit flag for OB-Oracle mode")
    private String oracleAutoCommitMode = "ON";
    /**
     * Query limit, default is 1000
     */
    @Min(value = 0, message = "Query limit can not be negative")
    @Max(value = Integer.MAX_VALUE, message = "Query limit can not be bigger than " + Integer.MAX_VALUE)
    @NotNull(message = "Query limit can not be null")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Count limit for resultSet")
    private Integer defaultQueryLimit = 1000;

    @NotBlank(message = "Default object dragging option can not be blank")
    @Pattern(regexp = "object_name|select_stmt|insert_stmt|update_stmt|delete_stmt", flags = Flag.CASE_INSENSITIVE,
            message = "Default object dragging option can only accept the value object_name|select_stmt|insert_stmt|update_stmt|delete_stmt")
    @ConfigMetaInfo(prefix = "sqlexecute", description = "Default object dragging option")
    private String defaultObjectDraggingOption = "object_name";

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
    private String sessionMode = "MultiSession";

    public UserConfig() {}

}
