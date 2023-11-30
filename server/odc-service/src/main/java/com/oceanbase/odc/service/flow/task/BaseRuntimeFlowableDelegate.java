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
package com.oceanbase.odc.service.flow.task;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.RetryExecutor;
import com.oceanbase.odc.core.flow.BaseFlowableDelegate;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.event.ServiceTaskStartedEvent;
import com.oceanbase.odc.service.flow.event.TaskInstanceCreatedListener;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.listener.ActiveTaskStatisticsListener;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTaskParameter;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.ScheduleSourceType;
import com.oceanbase.odc.service.task.schedule.TaskTaskJobDefinitionBuilder;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Runtime service task instance, you need to bind the service task node instance to provide all
 * operation handles related to the task
 *
 * @author yh263208
 * @date 2022-02-14 14:06
 * @since ODC_release_3.3.0
 */
@Slf4j
public abstract class BaseRuntimeFlowableDelegate<T> extends BaseFlowableDelegate implements Future<T> {

    @Getter
    private String activityId;
    @Getter
    private Long targetTaskInstanceId;
    @Getter
    private TaskType taskType;
    @Getter
    private Long flowInstanceId;
    @Getter
    private ExecutionStrategyConfig strategyConfig;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    private volatile T returnObject = null;
    private volatile Exception thrown = null;
    private volatile boolean done = false;
    private final CountDownLatch latch;
    private final RetryExecutor retryExecutor;
    private final TaskInstanceCreatedListener taskInstanceCreatedlistener;
    private final ActiveTaskStatisticsListener activeTaskStatisticsListener;
    @Autowired
    protected JobScheduler jobScheduler;

    public BaseRuntimeFlowableDelegate() {
        this.retryExecutor = RetryExecutor.builder().retryIntervalMillis(1000).retryTimes(3).build();
        this.latch = new CountDownLatch(1);
        this.taskInstanceCreatedlistener = new TaskInstanceCreatedListener(this);
        this.activeTaskStatisticsListener = new ActiveTaskStatisticsListener(this);
    }

    @PostConstruct
    public void init(EventPublisher eventPublisher) {
        eventPublisher.addEventListener(activeTaskStatisticsListener);
        eventPublisher.addEventListener(taskInstanceCreatedlistener);
    }

    @Override
    protected void run(DelegateExecution execution) throws Exception {
        Callable<T> callable;
        try {
            this.activityId = execution.getCurrentActivityId();
            initTargetTaskInstanceId(execution);
            // todo for smoke test
            if (this.taskType == TaskType.ASYNC) {
                Long taskId = FlowTaskUtil.getTaskId(execution);
                JobIdentity ji = JobIdentity.of(taskId, ScheduleSourceType.TASK_TASK, TaskType.SAMPLE.name());
                TaskTaskJobDefinitionBuilder factory = new TaskTaskJobDefinitionBuilder();

                JobDefinition jd = factory.build(ji);

                DefaultJobContext jobContext = (DefaultJobContext) jd.getJobContext();
                DatabaseChangeParameters parameters = FlowTaskUtil.getAsyncParameter(execution);


                SampleTaskParameter stp = new SampleTaskParameter();
                stp.setSqls(
                        SqlUtils.split(DialectType.OB_MYSQL, parameters.getSqlContent(), parameters.getDelimiter()));
                jobContext.setTaskParameters(JsonUtils.toJson(stp));
                jobScheduler.scheduleJobNow(jd);
                return;
            }

            callable = initCallable(execution);
            Verify.notNull(callable, "Callable");
            eventPublisher.publishEvent(new ServiceTaskStartedEvent(this, Thread.currentThread()));
        } catch (Exception e) {
            log.warn("Initialization task failed", e);
            thrown = e;
            done = true;
            latch.countDown();
            throw new ServiceTaskError(e);
        }
        try {
            preHandle(execution);
            returnObject = callable.call();
        } catch (Exception e) {
            log.warn("Service task execution failed", e);
            thrown = e;
            throw e;
        } finally {
            done = true;
            latch.countDown();
        }
    }

    protected void preHandle(DelegateExecution execution) {}

    @Override
    public void execute(DelegateExecution execution) {
        try {
            super.execute(execution);
        } finally {
            eventPublisher.removeEventListener(taskInstanceCreatedlistener);
            eventPublisher.removeEventListener(activeTaskStatisticsListener);
        }
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        this.latch.await();
        if (this.thrown != null) {
            throw new ExecutionException(this.thrown);
        }
        return this.returnObject;
    }

    @SuppressWarnings("all")
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        this.latch.await(timeout, unit);
        if (!this.done) {
            throw new TimeoutException();
        }
        if (this.thrown != null) {
            throw new ExecutionException(this.thrown);
        }
        return this.returnObject;
    }

    private void initTargetTaskInstanceId(DelegateExecution execution) {
        String processDefinitionId = execution.getProcessDefinitionId();
        Optional<Optional<Long>> retryOptional = retryExecutor.run(
                () -> flowableAdaptor.getFlowInstanceIdByProcessDefinitionId(processDefinitionId), Optional::isPresent);
        PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "processDefinitionId", processDefinitionId,
                retryOptional::isPresent);
        this.flowInstanceId = retryOptional.get().get();

        String activityId = execution.getCurrentActivityId();
        Optional<FlowTaskInstance> optional = flowableAdaptor.getTaskInstanceByActivityId(activityId, flowInstanceId);
        PreConditions.validExists(ResourceType.ODC_FLOW_TASK_INSTANCE, "activityId", activityId, optional::isPresent);
        FlowTaskInstance flowTaskInstance = optional.get();

        this.targetTaskInstanceId = flowTaskInstance.getId();
        this.taskType = flowTaskInstance.getTaskType();
        this.strategyConfig = flowTaskInstance.getStrategyConfig();
        flowTaskInstance.dealloc();
    }

    public void bindToFlowTaskInstance(@NonNull FlowTaskInstance taskInstance) {
        if (Objects.equals(taskInstance.getId(), targetTaskInstanceId)) {
            taskInstance.bindServiceTask(this);
            log.info("Monitor task instance creation and bind tasks, taskInstanceId={}, activityId={}",
                    taskInstance.getId(), activityId);
        }
    }

    protected void updateFlowInstanceStatus(@NonNull FlowStatus flowStatus) {
        this.retryExecutor.run(() -> {
            try {
                return flowInstanceRepository.updateStatusById(flowInstanceId, flowStatus);
            } catch (Exception e) {
                return null;
            }
        }, Objects::nonNull);
    }

    /**
     * Initialize the task. Due to the integration of flowable, the odc cannot control the instantiation
     * of the task, so the user needs to provide the instantiation process of the callable
     *
     * @param execution delegate of {@link org.flowable.engine.runtime.Execution}
     * @return task logic {@link Callable}
     */
    protected abstract Callable<T> initCallable(DelegateExecution execution);

}
