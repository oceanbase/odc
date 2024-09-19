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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MonitorAutoConfiguration.BusinessMeterRegistry;

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

@Component
@Slf4j
@Getter
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class MeterHolder implements MeterClear {

    private final GaugeHolder gaugeHolder;
    private final CounterHolder counterHolder;
    private final TimerHolder timerHolder;

    public MeterHolder(BusinessMeterRegistry meterRegistry,
            MonitorProperties monitorProperties) {
        this.gaugeHolder = new GaugeHolder(meterRegistry, monitorProperties);
        this.counterHolder = new CounterHolder(meterRegistry, monitorProperties);
        this.timerHolder = new TimerHolder(meterRegistry, monitorProperties);
    }

    @Override
    public void clearAfterPull() {
        this.timerHolder.clearAfterPull();
    }

    public static class GaugeHolder {
        private final Map<MeterKey, GaugeHolder.GaugeWrapper> GAUGE_MAP = new ConcurrentHashMap<>();
        private final MeterRegistry registry;
        private final MonitorProperties monitorProperties;

        public GaugeHolder(MeterRegistry registry, MonitorProperties monitorProperties) {
            this.registry = registry;
            this.monitorProperties = monitorProperties;
        }

        public void increment(MeterKey meterKey, Integer defaultValue) {
            GaugeHolder.GaugeWrapper wrapper = GAUGE_MAP.get(meterKey);
            if (wrapper == null) {
                if (GAUGE_MAP.size() >= monitorProperties.getMeter().getMaxGaugeMeterNumber()) {
                    log.error("Too many gauges to register, meterKey={}", meterKey);
                    return;
                }
                GAUGE_MAP.computeIfAbsent(meterKey, (key) -> new GaugeHolder.GaugeWrapper(null, null));
            }
            MeterKey lockKey = getLockKey(meterKey);
            synchronized (lockKey) {
                wrapper = GAUGE_MAP.get(meterKey);
                if (wrapper.value == null) {
                    Gauge gauge = registerGauge(meterKey);
                    wrapper.setValue(defaultValue);
                    wrapper.setGauge(gauge);
                }
                GAUGE_MAP.get(lockKey).increment(1);
            }
        }

        public void decrement(MeterKey meterKey, Integer defaultValue) {
            GaugeHolder.GaugeWrapper wrapper = GAUGE_MAP.get(meterKey);
            if (wrapper == null) {
                if (GAUGE_MAP.size() >= monitorProperties.getMeter().getMaxGaugeMeterNumber()) {
                    log.error("Too many gauges to register, meterKey={}", meterKey);
                    return;
                }
                GAUGE_MAP.computeIfAbsent(meterKey, (key) -> new GaugeHolder.GaugeWrapper(null, null));
            }
            MeterKey lockKey = getLockKey(meterKey);
            synchronized (lockKey) {
                wrapper = GAUGE_MAP.get(meterKey);
                if (wrapper.value == null) {
                    registerGauge(meterKey);
                    wrapper.setValue(defaultValue);
                }
                GAUGE_MAP.get(lockKey).decrement(1);
            }
        }

        private MeterKey getLockKey(MeterKey meterKey) {
            return GAUGE_MAP.keySet().stream().filter(meterKey::equals).findFirst()
                    .orElseThrow(IllegalAccessError::new);
        }

        private Gauge registerGauge(MeterKey meterKey) {
            Iterable<Tag> tags = meterKey.getTags();
            Gauge.Builder<Supplier<Number>> builder = Gauge.builder(meterKey.getMeterName().getMeterName(),
                    () -> GAUGE_MAP.get(meterKey).getValue())
                    .description(meterKey.getMeterName().getDescription());
            for (Tag tag : tags) {
                builder.tag(tag.getKey(), tag.getValue());
            }
            return builder.register(registry);
        }

        public Integer getSize() {
            return GAUGE_MAP.size();
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

    public static class CounterHolder {
        private final Map<MeterKey, CounterHolder.CounterWrapper> COUNTER_MAP = new ConcurrentHashMap<>();
        private final MeterRegistry registry;
        private final MonitorProperties monitorProperties;

        public CounterHolder(MeterRegistry registry, MonitorProperties monitorProperties) {
            this.registry = registry;
            this.monitorProperties = monitorProperties;
        }

        public void increment(MeterKey meterKey) {
            CounterHolder.CounterWrapper wrapper = COUNTER_MAP.get(meterKey);
            if (wrapper == null) {
                if (COUNTER_MAP.size() >= monitorProperties.getMeter().getMaxCounterMeterNumber()) {
                    log.error("Too many counter to register, meterKey={}", meterKey);
                    return;
                }
                COUNTER_MAP.computeIfAbsent(meterKey, (key) -> new CounterHolder.CounterWrapper(null));
            }
            MeterKey lockKey = getLockKey(meterKey);
            synchronized (lockKey) {
                wrapper = COUNTER_MAP.get(lockKey);
                if (wrapper.getCounter() == null) {
                    Counter counter = registerCounter(meterKey);
                    wrapper.setCounter(counter);
                }
                wrapper.getCounter().increment();
            }
        }

        private MeterKey getLockKey(MeterKey meterKey) {
            return COUNTER_MAP.keySet().stream().filter(meterKey::equals).findFirst()
                    .orElseThrow(IllegalAccessError::new);
        }

        private Counter registerCounter(MeterKey meterKey) {
            Builder builder = Counter.builder(meterKey.getMeterName().getMeterName())
                    .description(meterKey.getMeterName().getDescription());
            for (Tag tag : meterKey.getTags()) {
                builder.tag(tag.getKey(), tag.getValue());
            }
            return builder.register(registry);
        }

        public Integer getSize() {
            return COUNTER_MAP.size();
        }

        @Data
        @AllArgsConstructor
        static class CounterWrapper {
            private Counter counter;
        }
    }

    public static class TimerHolder implements MeterClear {
        private final Map<MeterKey, TimerHolder.TimerSampleHolder> METER_MAP = new ConcurrentHashMap<>();
        private final MeterRegistry registry;
        private final MonitorProperties monitorProperties;

        public TimerHolder(MeterRegistry registry, MonitorProperties monitorProperties) {
            this.registry = registry;
            this.monitorProperties = monitorProperties;
        }

        public void start(MeterKey meterKey) {
            TimerHolder.TimerSampleHolder sampleHolder = this.METER_MAP.get(meterKey);
            if (sampleHolder == null) {
                if (this.METER_MAP.size() >= monitorProperties.getMeter().getMaxTimerMeterNumber()) {
                    log.error("Too many Timer to register, meterKey={}", meterKey);
                    return;
                }
                // ensure lock same obj
                this.METER_MAP.computeIfAbsent(meterKey,
                        k -> new TimerHolder.TimerSampleHolder(null));
            }
            MeterKey lockKey = getLockKey(meterKey);
            synchronized (lockKey) {
                TimerHolder.TimerSampleHolder placeholder = this.METER_MAP.get(meterKey);
                if (placeholder.sample != null) {
                    return;
                }
                Timer.Sample start = Timer.start(this.registry);
                this.METER_MAP.put(lockKey,
                        new TimerHolder.TimerSampleHolder(start));
            }
        }

        public void record(MeterKey meterKey) {
            TimerHolder.TimerSampleHolder timerSampleHolder = this.METER_MAP.get(meterKey);
            if (timerSampleHolder == null) {
                return;
            }
            synchronized (timerSampleHolder) {
                timerSampleHolder = this.METER_MAP.get(meterKey);
                if (timerSampleHolder == null) {
                    return;
                }
                Iterable<Tag> tags = meterKey.getTags();
                Timer.Builder builder = Timer.builder(meterKey.getMeterName().getMeterName())
                        .description(meterKey.getMeterName().getDescription());
                for (Tag tag : tags) {
                    builder.tag(tag.getKey(), tag.getValue());
                }
                timerSampleHolder.stop(builder.register(registry));
            }
        }


        private MeterKey getLockKey(MeterKey meterKey) {
            return this.METER_MAP.keySet().stream().filter(meterKey::equals).findFirst()
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

        public Integer getSize() {
            return METER_MAP.size();
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
    public static class MeterKey {
        MeterName meterName;
        Iterable<Tag> tags;
        String uniqueKey;
        Boolean needRemove;

        public static MeterKey ofMeter(MeterName meter) {
            return new MeterKey(meter, Collections.emptyList(), null, false);
        }

        public static MeterKey ofMeter(MeterName meter, Tag... tags) {
            return new MeterKey(meter, Arrays.asList(tags), null, false);
        }

        public static MeterKey ofNeedRemoveMeter(MeterName meter, Tag... tags) {
            return new MeterKey(meter, Arrays.asList(tags), null, true);
        }

        public static MeterKey ofMeter(MeterName meter, String uniqueKey, Tag... tags) {
            return new MeterKey(meter, Arrays.asList(tags), uniqueKey, true);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MeterKey that = (MeterKey) o;
            if (!Objects.equals(meterName, that.meterName))
                return false;
            if (!Objects.equals(uniqueKey, that.uniqueKey)) {
                return false;
            }
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
            int result = Objects.hash(meterName, uniqueKey, needRemove);
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
