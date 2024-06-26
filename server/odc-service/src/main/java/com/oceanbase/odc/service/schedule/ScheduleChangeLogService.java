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
package com.oceanbase.odc.service.schedule;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.schedule.ScheduleChangeLogEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleChangeLogRepository;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLog;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLogMapper;

/**
 * @Author：tinker
 * @Date: 2024/6/8 15:28
 * @Descripition:
 */

@Service
public class ScheduleChangeLogService {
    @Autowired
    private ScheduleChangeLogRepository scheduleChangeLogRepository;

    private static final ScheduleChangeLogMapper mapper = ScheduleChangeLogMapper.INSTANCE;

    public ScheduleChangeLog createChangeLog(ScheduleChangeLog changeLog) {
        ScheduleChangeLogEntity entity = mapper.modelToEntity(changeLog);
        return mapper.entityToModel(scheduleChangeLogRepository.save(entity));
    }

    public ScheduleChangeLog getByIdAndScheduleId(Long id, Long scheduleId) {
        return scheduleChangeLogRepository.findByIdAndScheduleId(id, scheduleId).map(mapper::entityToModel)
                .orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_SCHEDULE_CHANGE_LOG, "id", id));
    }

    public List<ScheduleChangeLog> listByScheduleId(Long scheduleId) {
        return scheduleChangeLogRepository.findByScheduleId(scheduleId).stream().map(mapper::entityToModel).collect(
                Collectors.toList());
    }
}
