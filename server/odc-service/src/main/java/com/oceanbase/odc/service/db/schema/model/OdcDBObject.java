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
package com.oceanbase.odc.service.db.schema.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/3/29 11:49
 */
@Data
public class OdcDBObject {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private DBObjectType type;

    @JsonProperty(access = Access.READ_ONLY)
    private Database database;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    public static OdcDBObject fromEntity(DBObjectEntity entity) {
        OdcDBObject object = new OdcDBObject();
        if (entity == null) {
            return object;
        }
        object.setId(entity.getId());
        object.setName(entity.getName());
        object.setType(entity.getType());
        object.setOrganizationId(entity.getOrganizationId());
        object.setCreateTime(entity.getCreateTime());
        object.setUpdateTime(entity.getUpdateTime());
        if (entity.getDatabaseId() != null) {
            Database database = new Database();
            database.setId(entity.getDatabaseId());
            object.setDatabase(database);
        }
        return object;
    }

}
