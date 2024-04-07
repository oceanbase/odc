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
package com.oceanbase.odc.service.audit.util;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.audit.AuditEventMetaEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/14 下午4:55
 * @Description: []
 */
@Slf4j
public class AuditUtils {
    public static AuditEventType getEventTypeFromTaskType(TaskType taskType) {
        AuditEventType type = AuditEventType.UNKNOWN_TASK_TYPE;
        if (Objects.nonNull(taskType)) {
            switch (taskType) {
                case GENERATE_ROLLBACK:
                case SQL_CHECK:
                case ASYNC:
                    type = AuditEventType.ASYNC;
                    break;
                case EXPORT:
                    type = AuditEventType.EXPORT;
                    break;
                case EXPORT_RESULT_SET:
                    type = AuditEventType.EXPORT_RESULT_SET;
                    break;
                case IMPORT:
                    type = AuditEventType.IMPORT;
                    break;
                case MOCKDATA:
                    type = AuditEventType.MOCKDATA;
                    break;
                case PERMISSION_APPLY:
                    type = AuditEventType.PERMISSION_APPLY;
                    break;
                case SHADOWTABLE_SYNC:
                    type = AuditEventType.SHADOWTABLE_SYNC;
                    break;
                case PARTITION_PLAN:
                    type = AuditEventType.PARTITION_PLAN;
                    break;
                case ALTER_SCHEDULE:
                    type = AuditEventType.ALTER_SCHEDULE;
                    break;
                case ONLINE_SCHEMA_CHANGE:
                    type = AuditEventType.ONLINE_SCHEMA_CHANGE;
                    break;
                case APPLY_PROJECT_PERMISSION:
                    type = AuditEventType.APPLY_PROJECT_PERMISSION;
                    break;
                case APPLY_DATABASE_PERMISSION:
                    type = AuditEventType.APPLY_DATABASE_PERMISSION;
                    break;
                case APPLY_TABLE_PERMISSION:
                    type = AuditEventType.APPLY_TABLE_PERMISSION;
                    break;
                case STRUCTURE_COMPARISON:
                    type = AuditEventType.STRUCTURE_COMPARISON;
                    break;
                default:
                    type = AuditEventType.UNKNOWN_TASK_TYPE;
            }
        }
        return type;
    }

    public static AuditEventAction getSqlTypeFromResult(String sqlType) {
        AuditEventAction action = AuditEventAction.OTHERS;
        if (StringUtils.isNotEmpty(sqlType)) {
            switch (sqlType) {
                case "SELECT":
                    action = AuditEventAction.SELECT;
                    break;
                case "DELETE":
                    action = AuditEventAction.DELETE;
                    break;
                case "UPDATE":
                    action = AuditEventAction.UPDATE;
                    break;
                case "INSERT":
                    action = AuditEventAction.INSERT;
                    break;
                case "REPLACE":
                    action = AuditEventAction.REPLACE;
                    break;
                case "SET":
                    action = AuditEventAction.SET;
                    break;
                case "DROP":
                    action = AuditEventAction.DROP;
                    break;
                case "ALTER":
                    action = AuditEventAction.ALTER;
                    break;
                case "TRUNCATE":
                    action = AuditEventAction.TRUNCATE;
                    break;
                case "CREATE":
                    action = AuditEventAction.CREATE;
                    break;
                default:
                    action = AuditEventAction.OTHERS;
            }
        }
        return action;
    }

    public static List<AuditEventMetaEntity> createEntitiesByActions(AuditEventMetaEntity entity,
            List<AuditEventAction> actions) {
        List<AuditEventMetaEntity> returnVal = Lists.newArrayList();
        for (AuditEventAction action : actions) {
            AuditEventMetaEntity createdEntity = ObjectUtil.deepCopy(entity, AuditEventMetaEntity.class);
            createdEntity.setAction(action);
            returnVal.add(createdEntity);
        }
        return returnVal;
    }

    public static List<AuditEventMetaEntity> createEntitiesByTypes(AuditEventMetaEntity entity,
            List<AuditEventType> types) {
        List<AuditEventMetaEntity> returnVal = Lists.newArrayList();
        for (AuditEventType type : types) {
            AuditEventMetaEntity createdEntity = ObjectUtil.deepCopy(entity, AuditEventMetaEntity.class);
            createdEntity.setType(type);
            returnVal.add(createdEntity);
        }
        return returnVal;
    }



    public static AuditEventAction getActualActionForTask(AuditEventType type, AuditEventAction action) {
        if (action == AuditEventAction.CREATE_TASK) {
            switch (type) {
                case MOCKDATA:
                    return AuditEventAction.CREATE_MOCKDATA_TASK;
                case IMPORT:
                    return AuditEventAction.CREATE_IMPORT_TASK;
                case EXPORT:
                    return AuditEventAction.CREATE_EXPORT_TASK;
                case EXPORT_RESULT_SET:
                    return AuditEventAction.CREATE_EXPORT_RESULT_SET_TASK;
                case ASYNC:
                    return AuditEventAction.CREATE_ASYNC_TASK;
                case PERMISSION_APPLY:
                    return AuditEventAction.CREATE_PERMISSION_APPLY_TASK;
                case SHADOWTABLE_SYNC:
                    return AuditEventAction.CREATE_SHADOWTABLE_SYNC_TASK;
                case STRUCTURE_COMPARISON:
                    return AuditEventAction.CREATE_STRUCTURE_COMPARISON_TASK;
                case PARTITION_PLAN:
                    return AuditEventAction.CREATE_PARTITION_PLAN_TASK;
                case ALTER_SCHEDULE:
                    return AuditEventAction.CREATE_ALTER_SCHEDULE_TASK;
                case ONLINE_SCHEMA_CHANGE:
                    return AuditEventAction.CREATE_ONLINE_SCHEMA_CHANGE_TASK;
                case APPLY_PROJECT_PERMISSION:
                    return AuditEventAction.CREATE_APPLY_PROJECT_PERMISSION_TASK;
                case APPLY_DATABASE_PERMISSION:
                    return AuditEventAction.CREATE_APPLY_DATABASE_PERMISSION_TASK;
                case APPLY_TABLE_PERMISSION:
                    return AuditEventAction.CREATE_APPLY_TABLE_PERMISSION_TASK;
            }
        }
        if (action == AuditEventAction.STOP_TASK) {
            switch (type) {
                case MOCKDATA:
                    return AuditEventAction.STOP_MOCKDATA_TASK;
                case IMPORT:
                    return AuditEventAction.STOP_IMPORT_TASK;
                case EXPORT:
                    return AuditEventAction.STOP_EXPORT_TASK;
                case EXPORT_RESULT_SET:
                    return AuditEventAction.STOP_EXPORT_RESULT_SET_TASK;
                case ASYNC:
                    return AuditEventAction.STOP_ASYNC_TASK;
                case SHADOWTABLE_SYNC:
                    return AuditEventAction.STOP_SHADOWTABLE_SYNC_TASK;
                case STRUCTURE_COMPARISON:
                    return AuditEventAction.STOP_STRUCTURE_COMPARISON_TASK;
                case PARTITION_PLAN:
                    return AuditEventAction.STOP_PARTITION_PLAN_TASK;
                case ALTER_SCHEDULE:
                    return AuditEventAction.STOP_ALTER_SCHEDULE_TASK;
                case ONLINE_SCHEMA_CHANGE:
                    return AuditEventAction.STOP_ONLINE_SCHEMA_CHANGE_TASK;
                case APPLY_PROJECT_PERMISSION:
                    return AuditEventAction.STOP_APPLY_PROJECT_PERMISSION_TASK;
                case APPLY_DATABASE_PERMISSION:
                    return AuditEventAction.STOP_APPLY_DATABASE_PERMISSION_TASK;
                case APPLY_TABLE_PERMISSION:
                    return AuditEventAction.STOP_APPLY_TABLE_PERMISSION_TASK;
            }
        }
        if (action == AuditEventAction.EXECUTE_TASK) {
            switch (type) {
                case MOCKDATA:
                    return AuditEventAction.EXECUTE_MOCKDATA_TASK;
                case IMPORT:
                    return AuditEventAction.EXECUTE_IMPORT_TASK;
                case EXPORT:
                    return AuditEventAction.EXECUTE_EXPORT_TASK;
                case EXPORT_RESULT_SET:
                    return AuditEventAction.EXECUTE_EXPORT_RESULT_SET_TASK;
                case ASYNC:
                    return AuditEventAction.EXECUTE_ASYNC_TASK;
                case SHADOWTABLE_SYNC:
                    return AuditEventAction.EXECUTE_SHADOWTABLE_SYNC_TASK;
                case STRUCTURE_COMPARISON:
                    return AuditEventAction.EXECUTE_STRUCTURE_COMPARISON_TASK;
                case PARTITION_PLAN:
                    return AuditEventAction.EXECUTE_PARTITION_PLAN_TASK;
                case ALTER_SCHEDULE:
                    return AuditEventAction.EXECUTE_ALTER_SCHEDULE_TASK;
                case ONLINE_SCHEMA_CHANGE:
                    return AuditEventAction.EXECUTE_ONLINE_SCHEMA_CHANGE_TASK;
            }
        }
        if (action == AuditEventAction.APPROVE) {
            switch (type) {
                case MOCKDATA:
                    return AuditEventAction.APPROVE_MOCKDATA_TASK;
                case IMPORT:
                    return AuditEventAction.APPROVE_IMPORT_TASK;
                case EXPORT:
                    return AuditEventAction.APPROVE_EXPORT_TASK;
                case EXPORT_RESULT_SET:
                    return AuditEventAction.APPROVE_EXPORT_RESULT_SET_TASK;
                case ASYNC:
                    return AuditEventAction.APPROVE_ASYNC_TASK;
                case PERMISSION_APPLY:
                    return AuditEventAction.APPROVE_PERMISSION_APPLY_TASK;
                case SHADOWTABLE_SYNC:
                    return AuditEventAction.APPROVE_SHADOWTABLE_SYNC_TASK;
                case STRUCTURE_COMPARISON:
                    return AuditEventAction.APPROVE_STRUCTURE_COMPARISON_TASK;
                case PARTITION_PLAN:
                    return AuditEventAction.APPROVE_PARTITION_PLAN_TASK;
                case ALTER_SCHEDULE:
                    return AuditEventAction.APPROVE_ALTER_SCHEDULE_TASK;
                case ONLINE_SCHEMA_CHANGE:
                    return AuditEventAction.APPROVE_ONLINE_SCHEMA_CHANGE_TASK;
                case APPLY_PROJECT_PERMISSION:
                    return AuditEventAction.APPROVE_APPLY_PROJECT_PERMISSION_TASK;
                case APPLY_DATABASE_PERMISSION:
                    return AuditEventAction.APPROVE_APPLY_DATABASE_PERMISSION_TASK;
                case APPLY_TABLE_PERMISSION:
                    return AuditEventAction.APPROVE_APPLY_TABLE_PERMISSION_TASK;
            }
        }
        if (action == AuditEventAction.REJECT) {
            switch (type) {
                case MOCKDATA:
                    return AuditEventAction.REJECT_MOCKDATA_TASK;
                case IMPORT:
                    return AuditEventAction.REJECT_IMPORT_TASK;
                case EXPORT:
                    return AuditEventAction.REJECT_EXPORT_TASK;
                case EXPORT_RESULT_SET:
                    return AuditEventAction.REJECT_EXPORT_RESULT_SET_TASK;
                case ASYNC:
                    return AuditEventAction.REJECT_ASYNC_TASK;
                case PERMISSION_APPLY:
                    return AuditEventAction.REJECT_PERMISSION_APPLY_TASK;
                case SHADOWTABLE_SYNC:
                    return AuditEventAction.REJECT_SHADOWTABLE_SYNC_TASK;
                case STRUCTURE_COMPARISON:
                    return AuditEventAction.REJECT_STRUCTURE_COMPARISON_TASK;
                case PARTITION_PLAN:
                    return AuditEventAction.REJECT_PARTITION_PLAN_TASK;
                case ALTER_SCHEDULE:
                    return AuditEventAction.REJECT_ALTER_SCHEDULE_TASK;
                case ONLINE_SCHEMA_CHANGE:
                    return AuditEventAction.REJECT_ONLINE_SCHEMA_CHANGE_TASK;
                case APPLY_PROJECT_PERMISSION:
                    return AuditEventAction.REJECT_APPLY_PROJECT_PERMISSION_TASK;
                case APPLY_DATABASE_PERMISSION:
                    return AuditEventAction.REJECT_APPLY_DATABASE_PERMISSION_TASK;
                case APPLY_TABLE_PERMISSION:
                    return AuditEventAction.REJECT_APPLY_TABLE_PERMISSION_TASK;
            }
        }
        // 如果不是流程相关的 action，则返回原值
        return action;
    }

}
