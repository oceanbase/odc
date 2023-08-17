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
package com.oceanbase.odc.service.dml.model;

import lombok.Data;

/**
 * @author yizhou.xw
 * @version : OdcBatchDataModifyResp.java, v 0.1 2021-08-19 13:35
 */
@Data
public class BatchDataModifyResp {
    /**
     * schema 名称
     */
    private String schemaName;

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 生成的 SQL 语句
     */
    private String sql;

    /***
     * 提示
     */
    private String tip;

    /**
     * 尝试 CREATE 的行数
     */
    private int createRows;

    /**
     * 尝试 UPDATE 的行数
     */
    private int updateRows;

    /**
     * 尝试 DELETE 的行数
     */
    private int deleteRows;

    /**
     * 预计 CREATE 是否影响多行
     */
    private boolean createAffectedMultiRows;
    /**
     * 预计 UPDATE 是否影响多行
     */
    private boolean updateAffectedMultiRows;
    /**
     * 预计 DELETE 是否影响多行
     */
    private boolean deleteAffectedMultiRows;
}
