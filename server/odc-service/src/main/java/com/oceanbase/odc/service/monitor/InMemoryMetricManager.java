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

import static com.oceanbase.odc.service.monitor.MeterName.CONNECT_SESSION_ACTIVE_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.METER_COUNTER_HOLDER_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.METER_TIMER_HOLDER_COUNT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MonitorAutoConfiguration.BusinessMeterRegistry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Counter.Builder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Getter
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class InMemoryMetricManager implements MetricManager, InitializingBean {

    private final Map<MeterKey, CounterWrapper> COUNTER_MAP = new ConcurrentHashMap<>();
    private final Map<MeterKey, TimerSampleHolder> TIMER_MAP = new ConcurrentHashMap<>();
    @Autowired
    private BusinessMeterRegistry businessMeterRegistry;

    @Autowired
    private MonitorProperties monitorProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        registerGauge(MeterKey.ofMeter(METER_COUNTER_HOLDER_COUNT), this.COUNTER_MAP::size);
        registerGauge(MeterKey.ofMeter(METER_TIMER_HOLDER_COUNT), this.TIMER_MAP::size);
    }


    public void registerGauge(MeterKey meterKey, Supplier<Number> f) {
        MeterName meterName = meterKey.getMeterName();
        Iterable<Tag> tags = meterKey.getTags();
        Gauge.Builder<Supplier<Number>> builder = Gauge.builder(meterName.getMeterName(), f)
                .description(CONNECT_SESSION_ACTIVE_COUNT.getDescription());
        for (Tag tag : tags) {
            builder.tag(tag.getKey(), tag.getValue());
        }
        builder.register(businessMeterRegistry);
    }

    public void incrementCounter(MeterKey meterKey) {
        CounterWrapper wrapper = COUNTER_MAP.get(meterKey);
        if (wrapper == null) {
            if (COUNTER_MAP.size() >= monitorProperties.getMeter().getMaxCounterMeterNumber()) {
                log.error("Too many counter to register, meterKey={}", meterKey);
                return;
            }
            COUNTER_MAP.computeIfAbsent(meterKey, (key) -> new CounterWrapper(null));
        }
        MeterKey lockKey = getCounterLockKey(meterKey);
        synchronized (lockKey) {
            wrapper = COUNTER_MAP.get(lockKey);
            if (wrapper.getCounter() == null) {
                Counter counter = registerCounter(meterKey);
                wrapper.setCounter(counter);
            }
            wrapper.getCounter().increment();
        }
    }

    private MeterKey getCounterLockKey(MeterKey meterKey) {
        return COUNTER_MAP.keySet().stream().filter(meterKey::equals).findFirst()
                .orElseThrow(IllegalAccessError::new);
    }

    private Counter registerCounter(MeterKey meterKey) {
        Builder builder = Counter.builder(meterKey.getMeterName().getMeterName())
                .description(meterKey.getMeterName().getDescription());
        for (Tag tag : meterKey.getTags()) {
            builder.tag(tag.getKey(), tag.getValue());
        }
        return builder.register(businessMeterRegistry);
    }

    public void startTimer(MeterKey meterKey) {
        TimerSampleHolder sampleHolder = this.TIMER_MAP.get(meterKey);
        if (sampleHolder == null) {
            if (this.TIMER_MAP.size() >= monitorProperties.getMeter().getMaxTimerMeterNumber()) {
                log.error("Too many Timer to register, meterKey={}", meterKey);
                return;
            }
            // ensure lock same obj
            this.TIMER_MAP.computeIfAbsent(meterKey,
                    k -> new TimerSampleHolder(null));
        }
        MeterKey lockKey = getTimerLockKey(meterKey);
        synchronized (lockKey) {
            TimerSampleHolder placeholder = this.TIMER_MAP.get(meterKey);
            if (placeholder.sample != null) {
                return;
            }
            Timer.Sample start = Timer.start(this.businessMeterRegistry);
            this.TIMER_MAP.put(lockKey,
                    new TimerSampleHolder(start));
        }
    }


    public void recordTimer(MeterKey meterKey) {
        TimerSampleHolder timerSampleHolder = this.TIMER_MAP.get(meterKey);
        if (timerSampleHolder == null) {
            return;
        }
        synchronized (timerSampleHolder) {
            timerSampleHolder = this.TIMER_MAP.get(meterKey);
            if (timerSampleHolder == null) {
                return;
            }
            Iterable<Tag> tags = meterKey.getTags();
            Timer.Builder builder = Timer.builder(meterKey.getMeterName().getMeterName())
                    .description(meterKey.getMeterName().getDescription());
            for (Tag tag : tags) {
                builder.tag(tag.getKey(), tag.getValue());
            }
            timerSampleHolder.stop(builder.register(businessMeterRegistry));
        }
    }

    @Override
    public void evict() {
        clearNeedRemoveMeter();
    }

    private synchronized void clearNeedRemoveMeter() {
        this.TIMER_MAP.entrySet().stream().filter(e -> e.getKey().getNeedRemove())
                .filter(e -> e.getValue().markToRemove)
                .forEach(e -> {
                    this.TIMER_MAP.remove(e.getKey());
                    businessMeterRegistry.remove(e.getValue().getTimer());
                });
    }

    private MeterKey getTimerLockKey(MeterKey meterKey) {
        return this.TIMER_MAP.keySet().stream().filter(meterKey::equals).findFirst()
                .orElseThrow(IllegalAccessError::new);
    }

    @Getter
    private static class TimerSampleHolder {
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

    @Data
    @AllArgsConstructor
    private static class CounterWrapper {
        private Counter counter;
    }
}
