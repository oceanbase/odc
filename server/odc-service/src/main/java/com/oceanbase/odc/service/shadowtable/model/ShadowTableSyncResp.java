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

import java.util.List;

import com.oceanbase.odc.metadb.shadowtable.TableComparingEntity;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/9/19 下午2:15
 * @Description: []
 */
@Data
public class ShadowTableSyncResp {
    private Long id;
    private String allDDL;
    private List<TableComparing> tables;
    private boolean completed;
    private double progressPercentage;

    @Data
    public static class TableComparing {
        private Long id;
        private String originTableName;
        private String destTableName;
        private TableComparingResult comparingResult;
        private String originTableDDL;
        private String destTableDDL;
        private String comparingDDL;

        public static TableComparingEntity toEntity(TableComparing comparing) {
            TableComparingEntity entity = new TableComparingEntity();
            entity.setId(comparing.getId());
            entity.setOriginalTableName(comparing.getOriginTableName());
            entity.setOriginalTableDDL(comparing.getOriginTableDDL());
            entity.setDestTableName(comparing.getDestTableName());
            entity.setDestTableDDL(comparing.getDestTableDDL());
            entity.setComparingDDL(comparing.getComparingDDL());
            entity.setComparingResult(comparing.getComparingResult());
            return entity;
        }

        public static TableComparing toTableComparing(TableComparingEntity entity) {
            TableComparing comparing = toTableComparingWithoutDDL(entity);

            comparing.setOriginTableDDL(entity.getOriginalTableDDL());
            comparing.setDestTableDDL(entity.getDestTableDDL());
            comparing.setComparingDDL(entity.getComparingDDL());
            return comparing;
        }

        public static TableComparing toTableComparingWithoutDDL(TableComparingEntity entity) {
            TableComparing comparing = new TableComparing();
            comparing.setId(entity.getId());
            comparing.setComparingResult(entity.getSkipped() ? TableComparingResult.SKIP : entity.getComparingResult());
            comparing.setOriginTableName(entity.getOriginalTableName());
            comparing.setDestTableName(entity.getDestTableName());
            return comparing;
        }
    }
}
