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

import static com.oceanbase.odc.service.monitor.MonitorEvent.MonitorModule.DATASOURCE_MODULE;
import static com.oceanbase.odc.service.monitor.MonitorEvent.MonitorModule.JOB;
import static com.oceanbase.odc.service.monitor.MonitorEvent.MonitorModule.SCHEDULE_MODULE;

import org.springframework.context.ApplicationEvent;

import com.oceanbase.odc.service.monitor.datasource.DataSourseMonitorEventHandler;
import com.oceanbase.odc.service.monitor.datasource.DatasourceMonitorEventContext;
import com.oceanbase.odc.service.monitor.session.SessionMonitorContext;
import com.oceanbase.odc.service.monitor.session.SessionMonitorEventHandler;
import com.oceanbase.odc.service.monitor.task.flow.FlowMonitorEvent;
import com.oceanbase.odc.service.monitor.task.flow.FlowMonitorEventHandler;
import com.oceanbase.odc.service.monitor.task.job.TaskJobMonitorEvent;
import com.oceanbase.odc.service.monitor.task.job.TaskJobMonitorHandler;
import com.oceanbase.odc.service.monitor.task.schdule.ScheduleMonitorEvent;
import com.oceanbase.odc.service.monitor.task.schdule.ScheduleMonitorHandler;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class MonitorEvent<T> extends ApplicationEvent {

    private final MonitorModule module;

    private final T context;

    /**
     * Constructs a prototypical Event.
     *
     * @param context The object on which the Event initially occurred.
     * @param module
     * @throws IllegalArgumentException if source is null.
     */
    private MonitorEvent(@NonNull MonitorModule module, T context) {
        super(context);
        this.module = module;
        this.context = context;
    }

    public static MonitorEvent<SessionMonitorContext> createSessionMonitor(
            SessionMonitorContext context) {
        return new MonitorEvent<SessionMonitorContext>(MonitorModule.SESSION_MODULE, context);
    }


    public static MonitorEvent<DatasourceMonitorEventContext> createDatasourceMonitor(
            DatasourceMonitorEventContext context) {
        return new MonitorEvent<DatasourceMonitorEventContext>(DATASOURCE_MODULE, context);
    }

    public static MonitorEvent<FlowMonitorEvent> createFlowMonitor(FlowMonitorEvent.Action action, Long organizationId,
            String taskType, String taskId) {
        FlowMonitorEvent flowMonitorEvent = new FlowMonitorEvent(action, organizationId, taskId, taskType);
        return new MonitorEvent<>(MonitorModule.FLOW, flowMonitorEvent);
    }

    public static MonitorEvent<ScheduleMonitorEvent> createScheduleMonitorEvent(ScheduleMonitorEvent.Action action,
            ScheduleTaskType taskType, String scheduleId) {
        ScheduleMonitorEvent scheduleMonitorEvent = new ScheduleMonitorEvent(action, scheduleId, taskType.name());
        return new MonitorEvent<>(SCHEDULE_MODULE, scheduleMonitorEvent);
    }

    public static MonitorEvent<TaskJobMonitorEvent> createJobMonitorEvent(TaskJobMonitorEvent.Action action) {
        TaskJobMonitorEvent scheduleMonitorEvent = new TaskJobMonitorEvent(action);
        return new MonitorEvent<>(JOB, scheduleMonitorEvent);
    }


    public enum MonitorModule {
        FLOW(FlowMonitorEventHandler.class),
        JOB(TaskJobMonitorHandler.class),
        SCHEDULE_MODULE(ScheduleMonitorHandler.class),
        SESSION_MODULE(SessionMonitorEventHandler.class),
        DATASOURCE_MODULE(DataSourseMonitorEventHandler.class),
        ;

        @Getter
        private final Class<? extends MonitorEventHandler<?>> handler;

        MonitorModule(Class<? extends MonitorEventHandler<?>> handler) {
            this.handler = handler;
        }

    }
}
