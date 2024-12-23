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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.schedule.ScheduleChangeLogEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleChangeLogRepository;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLog;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLogMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;

/**
 * @Author：tinker
 * @Date: 2024/6/8 15:28
 * @Descripition:
 */

@Service
@SkipAuthorize("odc internal usage")
public class ScheduleChangeLogService {
    @Autowired
    private ScheduleChangeLogRepository scheduleChangeLogRepository;

    private static final ScheduleChangeLogMapper mapper = ScheduleChangeLogMapper.INSTANCE;

    public ScheduleChangeLog createChangeLog(ScheduleChangeLog changeLog) {
        ScheduleChangeLogEntity entity = mapper.modelToEntity(changeLog);
        return mapper.entityToModel(scheduleChangeLogRepository.save(entity));
    }

    public ScheduleChangeLog getByIdAndScheduleId(Long id, Long scheduleId) {
        ScheduleChangeLog changeLog = scheduleChangeLogRepository.findByIdAndScheduleId(id, scheduleId).map(
                mapper::entityToModel)
                .orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_SCHEDULE_CHANGELOG, "id", id));
        if (StringUtils.isNotEmpty(changeLog.getNewParameter())
                && StringUtils.isNotEmpty(changeLog.getPreviousParameters())) {
            JSONObject pre = JSONObject.parseObject(changeLog.getPreviousParameters());
            JSONObject curr = JSONObject.parseObject(changeLog.getNewParameter());
            removeCommonKeys(pre, curr);
            changeLog.setPreviousParameters(pre.toJSONString());
            changeLog.setNewParameter(curr.toJSONString());
            return changeLog;
        }
        return changeLog;
    }

    public ScheduleChangeLog getByFlowInstanceId(Long flowInstanceId) {
        return scheduleChangeLogRepository.findByFlowInstanceId(flowInstanceId).map(mapper::entityToModel)
                .orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_SCHEDULE_CHANGELOG, "flowInstanceId", flowInstanceId));
    }

    public List<ScheduleChangeLog> listByScheduleId(Long scheduleId) {
        return scheduleChangeLogRepository.findByScheduleId(scheduleId).stream().map(mapper::entityToModel).collect(
                Collectors.toList());
    }

    public void updateStatusById(Long id, ScheduleChangeStatus status) {
        scheduleChangeLogRepository.updateStatusById(id, status);
    }

    public void updateFlowInstanceIdById(Long id, Long flowInstanceId) {
        scheduleChangeLogRepository.updateFlowInstanceIdById(id, flowInstanceId);
    }

    public boolean hasApprovingChangeLog(Long scheduleId) {
        return listByScheduleId(scheduleId).stream().map(ScheduleChangeLog::getStatus)
                .anyMatch(ScheduleChangeStatus.APPROVING::equals);
    }


    public static void removeCommonKeys(JSONObject json1, JSONObject json2) {
        Iterator<String> keys = json1.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();

            if (json2.containsKey(key)) {
                Object value1 = json1.get(key);
                Object value2 = json2.get(key);

                if (isJsonString(value1.toString()) && isJsonString(value2.toString())) {
                    JSONObject nestedJson1 = JSON.parseObject(value1.toString());
                    JSONObject nestedJson2 = JSON.parseObject(value2.toString());
                    removeCommonKeys(nestedJson1, nestedJson2);

                    json1.put(key, nestedJson1);
                    json2.put(key, nestedJson2);
                } else if (value1.equals(value2)) {
                    keys.remove();
                    json2.remove(key);
                }
            }
        }
    }

    public static boolean isJsonString(String value) {
        try {
            JSON.parseObject(value, Feature.IgnoreNotMatch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
