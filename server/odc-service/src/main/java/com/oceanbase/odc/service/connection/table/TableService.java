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
package com.oceanbase.odc.service.connection.table;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.connection.TableEntity;
import com.oceanbase.odc.metadb.connection.TableRepository;
import com.oceanbase.odc.service.connection.table.model.Table;

import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: TableService Package: com.oceanbase.odc.service.connection.table Description:
 *
 * @Author: fenghao
 * @Create 2024/3/12 21:21
 * @Version 1.0
 */
@Service
@Validated
@Slf4j
@Authenticated
public class TableService {
    private final TableMapper tableMapper = TableMapper.INSTANCE;
    @Autowired
    private TableRepository tableRepository;

    @SkipAuthorize("permission check inside")
    public List<Table> getByDatabaseId(Long databaseId) {
        return tableRepository.findByDatabaseId(databaseId).stream()
                .map(TableMapper.INSTANCE::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("permission check inside")
    public List<Table> getByDatabaseIds(Set<Long> databaseIds) {
        return tableRepository.findByDatabaseIdIn(databaseIds).stream()
                .map(TableMapper.INSTANCE::entityToModel)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public Table save(Table table) {
        return TableMapper.INSTANCE
                .entityToModel(tableRepository.saveAndFlush(TableMapper.INSTANCE.modelToEntity(table)));
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public List<Table> saveAll(List<Table> tables) {
        List<TableEntity> tableEntityList = tableRepository
                .saveAllAndFlush(tables.stream().map(TableMapper.INSTANCE::modelToEntity).collect(Collectors.toList()));
        return tableEntityList.stream().map(TableMapper.INSTANCE::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("permission check inside")
    public Table getByDatabaseIdAndName(Long databaseId, String tableName) {
        TableEntity tableEntity = tableRepository.findByDatabaseIdAndName(databaseId, tableName);
        if (tableEntity == null) {
            return null;
        } else {
            return TableMapper.INSTANCE.entityToModel(tableEntity);
        }
    }
}
