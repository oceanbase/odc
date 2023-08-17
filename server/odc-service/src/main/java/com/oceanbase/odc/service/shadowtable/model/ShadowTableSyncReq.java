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
package com.oceanbase.odc.service.shadowtable.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.metadb.shadowtable.ShadowTableComparingTaskEntity;
import com.oceanbase.odc.metadb.shadowtable.TableComparingEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Data;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2022/9/19 下午2:13
 * @Description: []
 */
@Data
public class ShadowTableSyncReq {
    private Long connectionId;
    @JsonIgnore
    private ConnectionConfig connectionConfig;
    @JsonProperty(access = Access.READ_ONLY)
    private String schemaName;
    @NotNull
    @Valid
    private Long databaseId;
    private List<String> originTableNames;
    private List<String> destTableNames;


    public static class ShadowTableComparingMapper {
        public static ShadowTableComparingTaskEntity ofTaskEntity(@NonNull ShadowTableSyncReq req,
                @NonNull Long userId) {
            ShadowTableComparingTaskEntity entity = new ShadowTableComparingTaskEntity();
            entity.setConnectionId(req.getConnectionId());
            entity.setSchemaName(req.getSchemaName());
            entity.setDatabaseId(req.getDatabaseId());
            entity.setCreatorId(userId);
            entity.setLastModifierId(userId);
            return entity;
        }

        public static List<TableComparingEntity> ofComparingEntity(@NonNull ShadowTableSyncReq req,
                @NonNull Long userId) {
            List<TableComparingEntity> entities = new ArrayList<>();
            if (CollectionUtils.isEmpty(req.getOriginTableNames())
                    || CollectionUtils.isEmpty(req.getDestTableNames())) {
                return entities;
            }
            for (int i = 0; i < req.getOriginTableNames().size(); i++) {
                TableComparingEntity entity = new TableComparingEntity();
                entity.setComparingResult(TableComparingResult.WAITING);
                entity.setOriginalTableName(req.getOriginTableNames().get(i));
                entity.setDestTableName(req.getDestTableNames().get(i));
                entity.setSkipped(false);
                entities.add(entity);
            }
            return entities;
        }
    }


}
