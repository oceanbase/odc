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

package com.oceanbase.odc.core.alarm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.Nullable;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.alarm.AlarmEvent.AlarmLevel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class AlarmService {

    private List<AlarmEventListener> listeners = new ArrayList<>();

    public AlarmService() {
        ServiceLoader<AlarmEventListener> load = ServiceLoader.load(AlarmEventListener.class);
        log.info("加载ServiceLoader<AlarmEventListener>， service ==> ", load);
        Iterator<AlarmEventListener> iterator = load.iterator();
        while (iterator.hasNext()) {
            AlarmEventListener next = iterator.next();
            log.info("遍历 AlarmEventListener 中... ", next);
            log.debug("AlarmEventListener:" + next.getClass().getName() + "have been loaded");
            listeners.add(next);
        }
    }

    public void alarm(String eventName, Map<String, String> eventMessage) {
        monitor(eventName, eventMessage, AlarmLevel.ERROR, null);
    }

    public void alarm(String eventName, String eventMessage) {
        monitor(eventName, AlarmUtils.createAlarmMapBuilder().item(AlarmUtils.MESSAGE_NAME, eventMessage).build(),
                AlarmLevel.ERROR,
                null);
    }

    public void alarm(String eventName, Throwable e) {
        monitor(eventName, AlarmUtils.createAlarmMapBuilder().item(AlarmUtils.MESSAGE_NAME, e.getMessage()).build(),
                AlarmLevel.ERROR,
                e);
    }

    public void warn(String eventName, Map<String, String> eventMessage) {
        monitor(eventName, eventMessage, AlarmLevel.WARN, null);
    }

    public void warn(String eventName, String eventMessage) {
        monitor(eventName, AlarmUtils.createAlarmMapBuilder().item(AlarmUtils.MESSAGE_NAME, eventMessage).build(),
                AlarmLevel.WARN, null);
    }

    public void warn(String eventName, Throwable e) {
        monitor(eventName, AlarmUtils.createAlarmMapBuilder().item(AlarmUtils.MESSAGE_NAME, e.getMessage()).build(),
                AlarmLevel.WARN,
                e);
    }

    public void info(String eventName, Map<String, String> eventMessage) {
        monitor(eventName, eventMessage, AlarmLevel.INFO, null);
    }

    public void info(String eventName, String eventMessage) {
        monitor(eventName, AlarmUtils.createAlarmMapBuilder().item(AlarmUtils.MESSAGE_NAME, eventMessage).build(),
                AlarmLevel.INFO, null);
    }

    private void monitor(String eventName, Map<String, String> eventMessage, AlarmLevel level, @Nullable Throwable e) {

        String msg = String.format("eventName=%s, eventMessage=%s .", eventName, eventMessage);
        switch (level) {
            case INFO:
                if (e == null) {
                    log.info(msg);
                } else {
                    log.info(msg + "stack:" + getInlineStack(e));
                }
                break;
            case WARN:
                if (e == null) {
                    log.warn(msg);
                } else {
                    log.warn(msg + "stack:" + getInlineStack(e));
                }
                break;
            case ERROR:
                if (e == null) {
                    log.error(msg);
                } else {
                    log.error(msg + "stack:" + getInlineStack(e));
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        AlarmEvent alarmEvent = new AlarmEvent(eventName, eventMessage, level);
        log.info("接受到了一个告警：alarmEvent={}", JsonUtils.toJson(alarmEvent));
        doPublish(alarmEvent);
    }

    private void doPublish(AlarmEvent alarmEvent) {
        listeners.forEach(l -> l.alarm(alarmEvent));
    }


    private String getInlineStack(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();
        stackTrace = stackTrace.replace("\n", " ").replace("\r", " ").replace("\t", " ");
        return stackTrace;
    }
}
