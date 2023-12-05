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

package com.oceanbase.odc.service.common;

import org.springframework.context.ApplicationEvent;

import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OdcMonitor {

    public static void alarm(String monitorPoint, String monitorMessage) {
        monitor(monitorPoint, monitorMessage, MonitorLevel.ERROR);
    }

    public static void alarm(String monitorPoint, Throwable e) {
        monitor(monitorPoint, e.getMessage(), MonitorLevel.ERROR);
    }

    public static void warn(String monitorPoint, String monitorMessage) {
        monitor(monitorPoint, monitorMessage, MonitorLevel.WARN);
    }

    public static void warn(String monitorPoint, Throwable e) {
        monitor(monitorPoint, e.getMessage(), MonitorLevel.WARN);
    }

    public static void info(String monitorPoint, String monitorMessage) {
        monitor(monitorPoint, monitorMessage, MonitorLevel.INFO);
    }


    public enum MonitorLevel {
        INFO, WARN, ERROR
    }


    @Getter
    public static class MonitorEvent extends ApplicationEvent {

        private final String monitorPoint;

        private final String monitorMessage;

        private final MonitorLevel level;

        public MonitorEvent(String monitorPoint, String monitorMessage, MonitorLevel level) {
            super(monitorPoint);
            this.monitorPoint = monitorPoint;
            this.monitorMessage = monitorMessage;
            this.level = level;
        }
    }

    private static void monitor(String monitorPoint, String monitorMessage, MonitorLevel level) {
        String msg = String.format("monitorPoint=%s, monitorMessage=%s", monitorPoint, monitorMessage);
        switch (level) {
            case INFO:
                log.info(msg);
                break;
            case WARN:
                log.warn(msg);
                break;
            case ERROR:
                log.error(msg);
                break;
            default:
                throw new IllegalArgumentException();
        }
        SpringContextUtil.publishEvent(new MonitorEvent(monitorPoint, monitorMessage, level));
    }
}
