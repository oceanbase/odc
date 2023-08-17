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
package com.oceanbase.odc.common.util;

import java.io.Closeable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link TraceWatch}
 *
 * @author yh263208
 * @date 2022-05-06 01:17
 * @since ODC_release_3.3.1
 */
@Getter
public class TraceWatch implements Closeable {
    private boolean closed = false;
    private long totalTimeMillis;
    private final String id;
    private final long startTimeMillis;
    @Getter(AccessLevel.NONE)
    private final Map<Long, Stack<TraceStage>> threadId2Stages;
    private final List<TraceStage> stageList;

    public TraceWatch() {
        this("");
    }

    public TraceWatch(String id) {
        this.id = id;
        this.threadId2Stages = new ConcurrentHashMap<>();
        this.stageList = new LinkedList<>();
        this.startTimeMillis = System.currentTimeMillis();
    }

    public TraceStage start() throws IllegalStateException {
        return start("");
    }

    public TraceStage start(String taskName) throws IllegalStateException {
        validClosed();
        TraceStage stage = new DefaultTraceStage(this, taskName);
        Stack<TraceStage> stages = this.threadId2Stages
                .computeIfAbsent(Thread.currentThread().getId(), id -> new Stack<>());
        stages.push(stage);
        stage.start();
        return stage;
    }

    public EditableTraceStage startEditableStage(String taskName) {
        validClosed();
        EditableTraceStage stage = new EditableTraceStage(this, taskName);
        Stack<TraceStage> stages = this.threadId2Stages
                .computeIfAbsent(Thread.currentThread().getId(), id -> new Stack<>());
        stages.push(stage);
        stage.start();
        return stage;
    }

    public ResumeableStage startResumeableStage(String taskName) {
        validClosed();
        List<TraceStage> resumeables = getReuseableByTaskName(taskName);
        ResumeableStage stage =
                resumeables.isEmpty() ? new ResumeableStage(this, taskName) : (ResumeableStage) resumeables.get(0);
        Stack<TraceStage> stages = this.threadId2Stages
                .computeIfAbsent(Thread.currentThread().getId(), id -> new Stack<>());
        stages.push(stage);
        stage.start();
        return stage;
    }

    public TraceStage stop() throws IllegalStateException {
        validClosed();
        Stack<TraceStage> stages = this.threadId2Stages.get(Thread.currentThread().getId());
        if (stages == null) {
            throw new NullPointerException("TraceStage stack is null");
        }
        TraceStage topStage = stages.pop();
        if (topStage == null) {
            throw new IllegalStateException("No stage is going to be stopped");
        }
        if (stages.size() != 0) {
            stages.peek().addSubStage(topStage);
        } else {
            synchronized (this.stageList) {
                this.stageList.add(topStage);
            }
        }
        topStage.stop();
        return topStage;
    }

    public List<TraceStage> getByTaskName(@NonNull String taskName) {
        List<TraceStage> result = new ArrayList<>();
        List<TraceStage> stages = new ArrayList<>();
        Collection<Stack<TraceStage>> stacks = this.threadId2Stages.values();
        stacks.forEach(stack -> stages.addAll(new ArrayList<>(stack)));
        stages.forEach(stage -> result.addAll(stage.getStageByTaskName(taskName)));
        return result;
    }

    public List<TraceStage> getReuseableByTaskName(@NonNull String taskName) {
        return this.stageList.stream()
                .map(stage -> stage.getStageByTaskName(taskName))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public TraceStage suspend() throws IllegalStateException {
        return consume(TraceStage::suspend);
    }

    public TraceStage resume() {
        return consume(TraceStage::resume);
    }

    public long getTime(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(System.currentTimeMillis() - startTimeMillis, TimeUnit.MILLISECONDS);
    }

    public long getTotalTime(@NonNull TimeUnit timeUnit) {
        if (!this.closed) {
            throw new IllegalStateException("TraceWatch is still running");
        }
        return timeUnit.convert(this.totalTimeMillis, TimeUnit.MILLISECONDS);
    }

    public String shortSummary() {
        return "TraceWatch '" + getId() + "': running time = " + getTime(TimeUnit.MILLISECONDS) + " ms";
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder(shortSummary());
        sb.append('\n');
        sb.append("---------------------------------------------\n");
        sb.append("ms         %     Stage name\n");
        sb.append("---------------------------------------------\n");
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumIntegerDigits(9);
        nf.setGroupingUsed(false);
        NumberFormat pf = NumberFormat.getPercentInstance();
        pf.setMinimumIntegerDigits(3);
        pf.setGroupingUsed(false);
        for (TraceStage task : this.stageList) {
            sb.append(nf.format(task.getTime(TimeUnit.MILLISECONDS))).append("  ");
            sb.append(pf.format((double) task.getTime(TimeUnit.MILLISECONDS) / getTime(TimeUnit.MILLISECONDS)))
                    .append("  ");
            sb.append(task.getMessage()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(shortSummary());
        for (TraceStage task : this.stageList) {
            sb.append("; [").append(task.getMessage()).append("] took ").append(task.getTime(TimeUnit.MILLISECONDS))
                    .append(" ms");
            long percent = Math.round(100.0 * task.getTime(TimeUnit.MILLISECONDS) / getTime(TimeUnit.MILLISECONDS));
            sb.append(" = ").append(percent).append("%");
        }
        return sb.toString();
    }

    private void validClosed() {
        if (this.closed) {
            throw new IllegalStateException("TraceWatch is closed");
        }
    }

    private TraceStage consume(@NonNull Consumer<TraceStage> consumer) {
        validClosed();
        Stack<TraceStage> stages = this.threadId2Stages.get(Thread.currentThread().getId());
        if (stages == null) {
            throw new NullPointerException("TraceStage stack is null");
        }
        if (stages.size() == 0) {
            throw new IllegalStateException("No stage is going to be stopped");
        }
        TraceStage target = stages.peek();
        consumer.accept(target);
        return target;
    }

    @Override
    public void close() {
        synchronized (this.threadId2Stages) {
            this.threadId2Stages.forEach((key, stages) -> {
                if (!stages.isEmpty()) {
                    throw new IllegalStateException("Stages is not empty fot thread " + key);
                }
            });
            this.totalTimeMillis = System.currentTimeMillis() - this.startTimeMillis;
        }
        this.closed = true;
    }

    @Getter
    public static class DefaultTraceStage extends StopWatch implements TraceStage {
        private final String threadName;
        protected final TraceWatch target;
        protected final List<TraceStage> subStageList = new LinkedList<>();

        public DefaultTraceStage(@NonNull TraceWatch target) {
            this(target, null);
        }

        public DefaultTraceStage(@NonNull TraceWatch target, String message) {
            super(message);
            this.target = target;
            this.threadName = Thread.currentThread().getName();
        }

        @Override
        public void addSubStage(@NonNull TraceStage stage) {
            this.subStageList.add(stage);
        }

        @Override
        public void close() {
            target.validClosed();
            this.subStageList.forEach(stage -> {
                if (!stage.isStopped()) {
                    throw new IllegalStateException("Some stages are not stopped");
                }
            });
            DefaultTraceStage that = this;
            List<Long> threadIds = target.threadId2Stages.entrySet().stream().filter(entry -> {
                Stack<TraceStage> stages = entry.getValue();
                if (stages.size() == 0) {
                    return false;
                }
                return stages.peek() == that;
            }).map(Entry::getKey).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(threadIds)) {
                throw new IllegalStateException("TraceStage is not singleton");
            } else if (threadIds.size() > 1) {
                throw new IllegalStateException("TraceStage is not singleton");
            }
            Stack<TraceStage> stages = target.threadId2Stages.get(threadIds.iterator().next());
            TraceStage topStage = stages.pop();
            if (topStage != this) {
                throw new IllegalStateException("Unknown error");
            }
            if (stages.size() != 0) {
                stages.peek().addSubStage(topStage);
            } else {
                synchronized (target.stageList) {
                    target.stageList.add(topStage);
                }
            }
            topStage.stop();
        }

        @Override
        public List<TraceStage> getStageByTaskName(@NonNull String taskName) {
            List<TraceStage> result = new ArrayList<>();
            if (taskName.equals(getMessage())) {
                result.add(this);
            }
            for (TraceStage subStage : subStageList) {
                result.addAll(subStage.getStageByTaskName(taskName));
            }
            return result;
        }
    }

    @Getter
    public static class EditableTraceStage extends DefaultTraceStage {
        private long totalDurationMicroseconds = -1;
        private long startTimeMillis = -1;

        public EditableTraceStage(@NonNull TraceWatch target) {
            super(target, null);
        }

        public EditableTraceStage(@NonNull TraceWatch target, String message) {
            super(target, message);
        }

        public void adapt(StopWatch stopWatch) {
            if (!stopWatch.isStopped()) {
                stopWatch.stop();
            }
            setStartTime(stopWatch.getStartTime(), TimeUnit.MILLISECONDS);
            setTime(stopWatch.getNanoTime(), TimeUnit.NANOSECONDS);
        }

        public void setTime(long duration, TimeUnit timeUnit) {
            this.totalDurationMicroseconds = TimeUnit.MICROSECONDS.convert(duration, timeUnit);
        }

        public void setStartTime(long startTime, TimeUnit timeUnit) {
            this.startTimeMillis = TimeUnit.MILLISECONDS.convert(startTime, timeUnit);
        }

        @Override
        public long getStartTime() {
            return startTimeMillis == -1 ? super.getStartTime() : startTimeMillis;
        }

        @Override
        public long getTime(TimeUnit timeUnit) {
            if (this.totalDurationMicroseconds != -1) {
                return timeUnit.convert(this.totalDurationMicroseconds, TimeUnit.MICROSECONDS);
            }
            return super.getTime(timeUnit);
        }

        @Override
        public void suspend() {
            throw new UnsupportedOperationException("EditableStage can not suspend");
        }

        @Override
        public void resume() {
            throw new UnsupportedOperationException("EditableStage can not resume");
        }

    }

    public static class ResumeableStage extends DefaultTraceStage {
        public ResumeableStage(@NonNull TraceWatch target) {
            super(target);
        }

        public ResumeableStage(@NonNull TraceWatch target, String message) {
            super(target, message);
        }

        @Override
        public void start() {
            if (isSuspended()) {
                resume();
            } else {
                super.start();
            }
        }

        public void stop() {
            if (isStarted()) {
                suspend();
            }
        }
    }

}
