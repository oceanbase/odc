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
package com.oceanbase.odc.service.monitor.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.oceanbase.odc.service.monitor.MeterClear;
import com.oceanbase.odc.service.monitor.MonitorProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Counter.Builder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskMetrics implements MeterClear {

    private final MeterRegistry registry;
    @Getter
    private final TaskGaugeHolder taskGaugeHolder;
    @Getter
    private final TaskCounterHolder taskCounterHolder;
    @Getter
    private final TaskTimerHolder taskTimerHolder;

    private final MonitorProperties monitorProperties;

    public TaskMetrics(MeterRegistry meterRegistry, MonitorProperties monitorProperties) {
        this.registry = meterRegistry;
        this.monitorProperties = monitorProperties;
        this.taskGaugeHolder = new TaskGaugeHolder(registry, monitorProperties);
        this.taskCounterHolder = new TaskCounterHolder(registry, monitorProperties);
        this.taskTimerHolder = new TaskTimerHolder(registry, monitorProperties);
    }

    @Override
    public void clearAfterPull() {
        taskTimerHolder.clearAfterPull();
    }

    public static class TaskGaugeHolder {
        private final Map<TaskMeterKey, GaugeWrapper> GAUGE_MAP = new ConcurrentHashMap<>();
        private final MeterRegistry registry;
        private final MonitorProperties monitorProperties;

        public TaskGaugeHolder(MeterRegistry registry, MonitorProperties monitorProperties) {
            this.registry = registry;
            this.monitorProperties = monitorProperties;
        }

        public void increment(TaskMeterKey taskMeterKey, Integer defaultValue) {
            GaugeWrapper wrapper = GAUGE_MAP.get(taskMeterKey);
            if (wrapper == null) {
                if (GAUGE_MAP.size() >= monitorProperties.getTask().getMaxGaugeMeterNumber()) {
                    log.error("Too many gauges to register, taskMeterKey={}", taskMeterKey);
                    return;
                }
                GAUGE_MAP.computeIfAbsent(taskMeterKey, (key) -> new GaugeWrapper(null, null));
            }
            TaskMeterKey lockKey = getLockKey(taskMeterKey);
            synchronized (lockKey) {
                wrapper = GAUGE_MAP.get(taskMeterKey);
                if (wrapper.value == null) {
                    Gauge gauge = registerTaskGauge(taskMeterKey);
                    wrapper.setValue(defaultValue);
                    wrapper.setGauge(gauge);
                }
                GAUGE_MAP.get(lockKey).increment(1);
            }
        }

        public void decrement(TaskMeterKey taskMeterKey, Integer defaultValue) {
            GaugeWrapper wrapper = GAUGE_MAP.get(taskMeterKey);
            if (wrapper == null) {
                if (GAUGE_MAP.size() >= monitorProperties.getTask().getMaxGaugeMeterNumber()) {
                    log.error("Too many gauges to register, taskMeterKey={}", taskMeterKey);
                    return;
                }
                GAUGE_MAP.computeIfAbsent(taskMeterKey, (key) -> new GaugeWrapper(null, null));
            }
            TaskMeterKey lockKey = getLockKey(taskMeterKey);
            synchronized (lockKey) {
                wrapper = GAUGE_MAP.get(taskMeterKey);
                if (wrapper.value == null) {
                    registerTaskGauge(taskMeterKey);
                    wrapper.setValue(defaultValue);
                }
                GAUGE_MAP.get(lockKey).decrement(1);
            }
        }

        private TaskMeterKey getLockKey(TaskMeterKey taskMeterKey) {
            return GAUGE_MAP.keySet().stream().filter(taskMeterKey::equals).findFirst()
                    .orElseThrow(IllegalAccessError::new);
        }

        private Gauge registerTaskGauge(TaskMeterKey taskMeterKey) {
            Iterable<Tag> tags = taskMeterKey.getTags();
            Gauge.Builder<Supplier<Number>> builder = Gauge.builder(taskMeterKey.getMeterName().getMeterName(),
                    () -> GAUGE_MAP.get(taskMeterKey).getValue())
                    .description(taskMeterKey.getMeterName().getDescription());
            for (Tag tag : tags) {
                builder.tag(tag.getKey(), tag.getValue());
            }
            return builder.register(registry);
        }

        @Data
        @AllArgsConstructor
        static class GaugeWrapper {
            Integer value;
            Gauge gauge;

            public void increment(Integer increment) {
                this.value = this.value + increment;
            }

            public void decrement(Integer decrement) {
                this.value = this.value - decrement;
            }
        }
    }

    public static class TaskCounterHolder {
        private final Map<TaskMeterKey, CounterWrapper> COUNTER_MAP = new ConcurrentHashMap<>();
        private final MeterRegistry registry;
        private final MonitorProperties monitorProperties;

        public TaskCounterHolder(MeterRegistry registry, MonitorProperties monitorProperties) {
            this.registry = registry;
            this.monitorProperties = monitorProperties;
        }

        public void increment(TaskMeterKey taskMeterKey) {
            CounterWrapper wrapper = COUNTER_MAP.get(taskMeterKey);
            if (wrapper == null) {
                if (COUNTER_MAP.size() >= monitorProperties.getTask().getMaxCounterMeterNumber()) {
                    log.error("Too many counter to register, taskMeterKey={}", taskMeterKey);
                    return;
                }
                COUNTER_MAP.computeIfAbsent(taskMeterKey, (key) -> new CounterWrapper(null));
            }
            TaskMeterKey lockKey = getLockKey(taskMeterKey);
            synchronized (lockKey) {
                wrapper = COUNTER_MAP.get(lockKey);
                if (wrapper.getCounter() == null) {
                    Counter counter = registerTaskCounter(taskMeterKey);
                    wrapper.setCounter(counter);
                }
                wrapper.getCounter().increment();
            }
        }

        private TaskMeterKey getLockKey(TaskMeterKey taskMeterKey) {
            return COUNTER_MAP.keySet().stream().filter(taskMeterKey::equals).findFirst()
                    .orElseThrow(IllegalAccessError::new);
        }

        private Counter registerTaskCounter(TaskMeterKey taskMeterKey) {
            Builder builder = Counter.builder(taskMeterKey.getMeterName().getMeterName())
                    .description(taskMeterKey.getMeterName().getDescription());
            for (Tag tag : taskMeterKey.getTags()) {
                builder.tag(tag.getKey(), tag.getValue());
            }
            return builder.register(registry);
        }

        @Data
        @AllArgsConstructor
        static class CounterWrapper {
            private Counter counter;
        }
    }

    public static class TaskTimerHolder implements MeterClear {
        private final Map<TaskMeterKey, TimerSampleHolder> METER_MAP = new ConcurrentHashMap<>();
        private final MeterRegistry registry;
        private final MonitorProperties monitorProperties;

        public TaskTimerHolder(MeterRegistry registry, MonitorProperties monitorProperties) {
            this.registry = registry;
            this.monitorProperties = monitorProperties;
        }

        public void start(TaskMeterKey taskMeterKey) {
            TimerSampleHolder longTaskTimerHolder = this.METER_MAP.get(taskMeterKey);
            if (longTaskTimerHolder == null) {
                if (this.METER_MAP.size() >= monitorProperties.getTask().getMaxTimerMeterNumber()) {
                    log.error("Too many Timer to register, taskMeterKey={}", taskMeterKey);
                    return;
                }
                // ensure lock same obj
                this.METER_MAP.computeIfAbsent(taskMeterKey,
                        k -> new TimerSampleHolder(null));
            }
            TaskMeterKey lockKey = getLockKey(taskMeterKey);
            synchronized (lockKey) {
                TimerSampleHolder placeholder = this.METER_MAP.get(taskMeterKey);
                if (placeholder.sample != null) {
                    return;
                }
                Timer.Sample start = Timer.start(this.registry);
                this.METER_MAP.put(lockKey,
                        new TimerSampleHolder(start));
            }
        }

        public void record(TaskMeterKey taskMeterKey) {
            TimerSampleHolder longTaskTimerHolder = this.METER_MAP.get(taskMeterKey);
            if (longTaskTimerHolder == null) {
                return;
            }
            synchronized (longTaskTimerHolder) {
                longTaskTimerHolder = this.METER_MAP.get(taskMeterKey);
                if (longTaskTimerHolder == null) {
                    return;
                }
                Iterable<Tag> tags = taskMeterKey.getTags();
                Timer.Builder builder = Timer.builder(taskMeterKey.getMeterName().getMeterName())
                        .description(taskMeterKey.getMeterName().getDescription());
                for (Tag tag : tags) {
                    builder.tag(tag.getKey(), tag.getValue());
                }
                longTaskTimerHolder.stop(builder.register(registry));
            }
        }


        private TaskMeterKey getLockKey(TaskMeterKey taskMeterKey) {
            return this.METER_MAP.keySet().stream().filter(taskMeterKey::equals).findFirst()
                    .orElseThrow(IllegalAccessError::new);
        }

        @Override
        public synchronized void clearAfterPull() {
            this.METER_MAP.entrySet().stream().filter(e -> e.getKey().needRemove)
                    .filter(e -> e.getValue().getMarkToRemove())
                    .forEach(e -> {
                        this.METER_MAP.remove(e.getKey());
                        registry.remove(e.getValue().getTimer());
                    });
        }

        @Getter
        public static class TimerSampleHolder {
            Timer.Sample sample;
            Timer timer;
            Boolean markToRemove = false;

            protected TimerSampleHolder(Timer.Sample sample) {
                this.sample = sample;
            }

            protected synchronized void stop(Timer timer) {
                this.timer = timer;
                this.sample.stop(timer);
                this.markToRemove = true;
            }
        }
    }

    @Value
    public static class TaskMeterKey {
        TaskMeters meterName;
        Iterable<Tag> tags;
        Boolean needRemove;

        public static TaskMeterKey ofTaskType(TaskMeters meter, String taskType) {
            return new TaskMeterKey(meter, Collections.singletonList(Tag.of("taskType", taskType)), false);
        }

        public static TaskMeterKey ofTaskTypeId(TaskMeters meter, String taskType, String taskId) {
            return new TaskMeterKey(meter,
                    Arrays.asList(Tag.of("taskType", taskType), Tag.of("taskId", taskId)), true);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TaskMeterKey that = (TaskMeterKey) o;
            if (!Objects.equals(meterName, that.meterName))
                return false;
            if (!Objects.equals(needRemove, that.needRemove)) {
                return false;
            }
            // Convert tags to sets for order-insensitive comparison
            Set<Tag> thisTagSet = toTagSet(this.tags);
            Set<Tag> thatTagSet = toTagSet(that.tags);

            return Objects.equals(thisTagSet, thatTagSet);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(meterName, needRemove);
            Set<Tag> tagSet = toTagSet(tags);
            result = 31 * result + tagSet.hashCode();
            return result;
        }

        private Set<Tag> toTagSet(Iterable<Tag> tags) {
            Set<Tag> tagSet = new HashSet<>();
            if (tags != null) {
                for (Tag tag : tags) {
                    tagSet.add(tag);
                }
            }
            return tagSet;
        }

    }
}
