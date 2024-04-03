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
package com.oceanbase.odc.service.db.object.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.metadb.dbobject.DBColumnEntity;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/3/29 11:57
 */
@Data
public class OdcDBColumn {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private OdcDBObject dbObject;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    public static OdcDBColumn fromEntity(DBColumnEntity entity) {
        OdcDBColumn column = new OdcDBColumn();
        if (entity == null) {
            return column;
        }
        column.setId(entity.getId());
        column.setName(entity.getName());
        column.setOrganizationId(entity.getOrganizationId());
        column.setCreateTime(entity.getCreateTime());
        column.setUpdateTime(entity.getUpdateTime());
        if (entity.getObjectId() != null) {
            OdcDBObject object = new OdcDBObject();
            object.setId(entity.getObjectId());
            column.setDbObject(object);
        }
        return column;
    }

}
