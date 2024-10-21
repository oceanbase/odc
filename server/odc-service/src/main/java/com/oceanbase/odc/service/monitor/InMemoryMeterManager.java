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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
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
public class InMemoryMeterManager implements MeterManager, InitializingBean {

    private final Map<MeterKey, CounterWrapper> COUNTER_MAP = new ConcurrentHashMap<>();
    private final Map<Pair<String, MeterKey>, TimerSampleHolder> TIMER_SAMPLE_MAP = new ConcurrentHashMap<>();
    @Autowired
    private BusinessMeterRegistry businessMeterRegistry;
    @Autowired
    private MonitorProperties monitorProperties;

    private Counter overCounterMaxCounter;

    private Counter overTimmerMaxCounter;

    @Override
    public void afterPropertiesSet() throws Exception {
        registerGauge(MeterKey.ofMeter(DefaultMeterName.METER_COUNTER_HOLDER_COUNT), this.COUNTER_MAP::size);
        registerGauge(MeterKey.ofMeter(DefaultMeterName.METER_TIMER_HOLDER_COUNT), this.TIMER_SAMPLE_MAP::size);
        overCounterMaxCounter =
                registerCounter(MeterKey.ofMeter(
                        DefaultMeterName.METER_OVER_MAX_REGISTER_COUNT, Tag.of("meter", "counter")));
        overTimmerMaxCounter =
                registerCounter(
                        MeterKey.ofMeter(DefaultMeterName.METER_OVER_MAX_REGISTER_COUNT, Tag.of("meter", "timer")));
    }


    public boolean registerGauge(MeterKey meterKey, Supplier<Number> f) {
        MeterName meterName = meterKey.getMeterName();
        Iterable<Tag> tags = meterKey.getTags();
        Gauge.Builder<Supplier<Number>> builder = Gauge.builder(meterName.getName(), f)
                .description(meterName.getDescription());
        for (Tag tag : tags) {
            builder.tag(tag.getKey(), tag.getValue());
        }
        builder.register(businessMeterRegistry);
        log.info(String.format("Register gauge, meterKey=[%s]", meterKey));
        return true;
    }

    public boolean incrementCounter(MeterKey meterKey) {
        CounterWrapper wrapper = COUNTER_MAP.get(meterKey);
        if (wrapper == null) {
            if (COUNTER_MAP.size() >= monitorProperties.getMeter().getMaxCounterMeterNumber()) {
                log.error("Too many counter to register, meterKey={}", meterKey);
                overCounterMaxCounter.increment();
                return false;
            }
            wrapper = COUNTER_MAP.computeIfAbsent(meterKey, (key) -> new CounterWrapper(null));
        }
        synchronized (wrapper) {
            if (wrapper.getCounter() == null) {
                Counter counter = registerCounter(meterKey);
                wrapper.setCounter(counter);
            }
            wrapper.getCounter().increment();
            return true;
        }
    }


    public Counter registerCounter(MeterKey meterKey) {
        Builder builder = Counter.builder(meterKey.getMeterName().getName())
                .description(meterKey.getMeterName().getDescription());
        for (Tag tag : meterKey.getTags()) {
            builder.tag(tag.getKey(), tag.getValue());
        }
        Counter register = builder.register(businessMeterRegistry);
        log.info(String.format("Register counter, meterKey=[%s]", register));
        return register;
    }

    public boolean startTimerSample(String sampleKey, MeterKey meterKey) {
        Pair<String, MeterKey> sampleMapKey = Pair.of(sampleKey, meterKey);
        TimerSampleHolder sampleHolder = this.TIMER_SAMPLE_MAP.get(sampleMapKey);
        if (sampleHolder == null) {
            if (this.TIMER_SAMPLE_MAP.size() >= monitorProperties.getMeter().getMaxTimerMeterNumber()) {
                log.error("Too many Timer to register, meterKey={}", meterKey);
                overTimmerMaxCounter.increment();
                return false;
            }
            sampleHolder = this.TIMER_SAMPLE_MAP.computeIfAbsent(sampleMapKey,
                    k -> new TimerSampleHolder(null));
        }
        synchronized (sampleHolder) {
            if (sampleHolder.sample != null) {
                return true;
            }
            Timer.Sample start = Timer.start(this.businessMeterRegistry);
            sampleHolder.setSample(start);
            log.info(String.format("Start a Timer Sample for MeterKey=%s", meterKey));
            return true;
        }
    }

    public boolean recordTimerSample(String sampleKey, MeterKey meterKey) {
        Pair<String, MeterKey> sampleMapKey = Pair.of(sampleKey, meterKey);
        TimerSampleHolder timerSampleHolder = this.TIMER_SAMPLE_MAP.get(sampleMapKey);
        if (timerSampleHolder == null) {
            return false;
        }
        synchronized (timerSampleHolder) {
            timerSampleHolder = this.TIMER_SAMPLE_MAP.get(sampleMapKey);
            if (timerSampleHolder == null) {
                return false;
            }
            Iterable<Tag> tags = meterKey.getTags();
            Timer.Builder builder = Timer.builder(meterKey.getMeterName().getName())
                    .description(meterKey.getMeterName().getDescription());
            for (Tag tag : tags) {
                builder.tag(tag.getKey(), tag.getValue());
            }
            timerSampleHolder.stop(builder.register(businessMeterRegistry));
            log.info(String.format("Record a Timer Sample for MeterKey=%s", meterKey));
            this.TIMER_SAMPLE_MAP.remove(sampleMapKey);
            return true;
        }
    }


    @Data
    private static class TimerSampleHolder {
        Timer.Sample sample;

        protected TimerSampleHolder(Timer.Sample sample) {
            this.sample = sample;
        }

        protected synchronized void stop(Timer timer) {
            this.sample.stop(timer);
        }
    }

    @Data
    @AllArgsConstructor
    private static class CounterWrapper {
        private Counter counter;
    }
}
