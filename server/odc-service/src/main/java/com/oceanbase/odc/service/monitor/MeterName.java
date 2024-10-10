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
package com.oceanbase.odc.service.monitor;

import lombok.Getter;

@Getter
public enum MeterName {

    // stateful route
    STATEFUL_ROUTE_COUNT("stateful.route.count", "stateful route count"),
    STATEFUL_ROUTE_UNHEALTHY_COUNT("stateful.route.unhealthy.count", "stateful route unhealthy count"),
    // flow module
    FLOW_CREATED_COUNT("flow.created.count", "flow created count"),
    FLOW_TASK_START_COUNT("flow.task.start.count", "flow task start count"),
    FLOW_TASK_FAILED_COUNT("flow.task.failed.count", "flow task failed"),
    FLOW_TASK_SUCCESS_COUNT("flow.task.success.count", "flow task success count"),
    FLOW_TASK_DURATION("flow.task.duration", "flow task duration"),

    // schedule
    SCHEDULE_START_COUNT("schedule.start.count", "schedule start count"),
    SCHEDULE_FAILED_COUNT("schedule.failed.count", "schedule failed count"),
    SCHEDULE_SUCCESS_COUNT("schedule.success.count", "schedule success count"),
    SCHEDULE_TASK_DURATION("schedule.task.duration", "schedule task duration"),
    SCHEDULE_INTERRUPTED_COUNT("schedule.interrupted.count", "schedule interrupted count"),
    SCHEDULE_ENABLED_COUNT("schedule.enabled.count", "schedule enabled count"),
    // job
    JOB_START_SUCCESS_COUNT("job.start.success.count", "job start success count"),
    JOB_START_FAILED_COUNT("job.start.failed.count", "job start failed"),
    JOB_STOP_SUCCESS_COUNT("job.stop.success.count", "job stop success count"),
    JOB_STOP_FAILED_COUNT("job.stop.failed.count", "job stop failed"),
    JOB_DESTROY_SUCCESS_COUNT("job.destroy.success.count", "job destroy success count"),
    JOB_DESTROY_FAILED_COUNT("job.destroy.failed.count", "job destroy failed count"),

    // session module
    CONNECT_SESSION_ACTIVE_COUNT("connect.session.active.count", "connect session active count"),
    CONNECT_SESSION_EXPIRED_COUNT("connect.session.expire.count", "connect session expired count"),
    CONNECT_SESSION_EXPIRED_FAILED_COUNT("connect.session.expire.failed_count", "connect session expired failed"),
    CONNECT_SESSION_TOTAL("connect.session.total", "connect session total"),
    CONNECT_SESSION_CREATED_FAILED_COUNT("connect.session.create.failed.count", "connect session created failed"),
    CONNECT_SESSION_DURATION_TIME("connect.session.duration.time", "connect session duration time"),
    CONNECT_SESSION_GET_COUNT("connect.session.get.count", "connect session get count"),
    CONNECT_SESSION_DELETE_SUCCESS_COUNT("connect.session.delete.success.count", "connect session get count"),
    CONNECT_SESSION_DELETE_FAILED_COUNT("connect.session.delete.failed.count", "connect session get count"),
    CONNECT_SESSION_GET_FAILED_COUNT("connect.session.get.failed.count", "connect session get failed count"),

    // datasource
    DATASOURCE_GET_CONNECTION_FAILED_COUNT("datasource.get.connection.failed.count",
            "datasource get connection failed count"),

    // meter holder;

    METER_COUNTER_HOLDER_COUNT("meter.counter.holder.count", "meter counter holder count"),
    METER_TIMER_HOLDER_COUNT("meter.timer.holder.count", "meter timer holder count"),
    METER_OVER_MAX_REGISTER_COUNT("meter.over.max.register", "meter over max register"),
    ;

    private final String meterName;
    private final String description;

    MeterName(String meterName, String description) {
        this.meterName = meterName;
        this.description = description;
    }
}
