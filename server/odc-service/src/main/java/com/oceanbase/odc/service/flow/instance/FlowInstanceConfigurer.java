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
package com.oceanbase.odc.service.flow.instance;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.Task;

import com.oceanbase.odc.core.flow.BaseExecutionListener;
import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.core.flow.builder.BaseProcessNodeBuilder;
import com.oceanbase.odc.core.flow.builder.BaseTaskBuilder;
import com.oceanbase.odc.core.flow.builder.ConditionSequenceFlowBuilder;
import com.oceanbase.odc.core.flow.builder.EndEventBuilder;
import com.oceanbase.odc.core.flow.builder.ErrorBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.ExclusiveGatewayBuilder;
import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.builder.ServiceTaskBuilder;
import com.oceanbase.odc.core.flow.builder.TimerBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.UserTaskBuilder;
import com.oceanbase.odc.core.flow.graph.GraphConfigurer;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.listener.ApprovalTaskExpiredListener;
import com.oceanbase.odc.service.flow.listener.BaseTaskBindUserTaskListener;
import com.oceanbase.odc.service.flow.listener.BaseTaskExecutingCompleteListener;
import com.oceanbase.odc.service.flow.listener.GatewayExecutingCompleteListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskExecutingCompleteListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskPendingExpiredListener;
import com.oceanbase.odc.service.flow.listener.ServiceTaskPendingListener;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;
import com.oceanbase.odc.service.flow.task.CreateExternalApprovalTask;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Configurer for {@link FlowInstance} used to build a {@link FlowInstance}
 *
 * @author yh263208
 * @date 2022-02-20 23:24
 * @since ODC_release_3.3.0
 * @see GraphConfigurer
 */
@Slf4j
@Getter
public class FlowInstanceConfigurer extends GraphConfigurer<FlowInstance, BaseFlowNodeInstance> {

    /**
     * 在实际的配置过程中，可能会出现多次操作同一个流程节点（例如多次 next 同一节点）， 原理上要求这种情况下拓扑图中链接到的是同一个对象，这种场景下就必须通过accessor
     * 根据名字获取到同一个对象才行。
     */
    private final ProcessNodeBuilderAccessor accessor;
    /**
     * Built-in {@link FlowableProcessBuilder} of {@link FlowInstance}
     */
    private final FlowableProcessBuilder targetProcessBuilder;
    private static final float DEFAULT_EDGE_WEIGHT = 1;
    protected final FlowableAdaptor flowableAdaptor;
    /**
     * Current {@link ExecutionConfigurer} of target {@link FlowableProcessBuilder}
     */
    protected final ExecutionConfigurer targetExecution;
    /**
     * 构造流程拓扑图时需要设定流程节点和{@code flowable}节点之间的关联，这种关联原理上只需要在
     * 流程创建的时候设定一次，由于流程的创建和加载都是用的是{@link FlowInstanceConfigurer} 因此在加载时就需要规定不进行绑定，这个标志位就是做这个标记的。
     */
    protected final boolean requiresActivityIdAndName;

    /**
     * This constructor can not be invoked by user
     *
     * @param flowInstance target {@link FlowInstance}
     */
    protected FlowInstanceConfigurer(@NonNull FlowInstance flowInstance,
            @NonNull FlowableProcessBuilder targetProcessBuilder, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull ProcessNodeBuilderAccessor accessor, boolean requiresActivityIdAndName) {
        super(flowInstance);
        this.targetExecution = targetProcessBuilder.newExecution();
        this.targetProcessBuilder = targetProcessBuilder;
        this.flowableAdaptor = flowableAdaptor;
        this.accessor = accessor;
        this.requiresActivityIdAndName = requiresActivityIdAndName;
    }

    /**
     * This constructor can not be invoked by user
     *
     * @param flowInstance target {@link FlowInstance}
     */
    protected FlowInstanceConfigurer(@NonNull FlowInstance flowInstance,
            @NonNull FlowableProcessBuilder targetProcessBuilder, @NonNull ExecutionConfigurer targetExecution,
            @NonNull FlowableAdaptor flowableAdaptor, @NonNull ProcessNodeBuilderAccessor accessor,
            boolean requiresActivityIdAndName) {
        super(flowInstance);
        this.targetExecution = targetExecution;
        this.targetProcessBuilder = targetProcessBuilder;
        this.flowableAdaptor = flowableAdaptor;
        this.accessor = accessor;
        this.requiresActivityIdAndName = requiresActivityIdAndName;
    }

    public FlowInstanceConfigurer next(@NonNull FlowApprovalInstance nextNode) {
        return next(nextNode, (Consumer<UserTaskBuilder>) userTaskBuilder -> {
            userTaskBuilder.addTaskListener(BaseTaskBindUserTaskListener.class);
            userTaskBuilder.addExecutionListener(BaseTaskExecutingCompleteListener.class);
            Integer expireIntervalSeconds = nextNode.getExpireIntervalSeconds();
            if (expireIntervalSeconds == null || expireIntervalSeconds <= 0) {
                return;
            }
            TimerBoundaryEventBuilder timerBuilder = setExpireSeconds(nextNode, userTaskBuilder, expireIntervalSeconds);
            timerBuilder.addExecutionListener(ApprovalTaskExpiredListener.class);
        });
    }

    public FlowInstanceConfigurer next(@NonNull FlowTaskInstance nextNode) {
        return next(nextNode, serviceTaskBuilder -> {
            serviceTaskBuilder.addExecutionListener(ServiceTaskExecutingCompleteListener.class);
            serviceTaskBuilder.setAsynchronous(true);
        }, userTaskBuilder -> {
            userTaskBuilder.addExecutionListener(ServiceTaskPendingListener.class);
            int waitExecExpireIntervalSeconds = nextNode.getStrategyConfig().getPendingExpireIntervalSeconds();
            if (waitExecExpireIntervalSeconds <= ExecutionStrategyConfig.INVALID_EXPIRE_INTERVAL_SECOND) {
                return;
            }
            TimerBoundaryEventBuilder timerBuilder =
                    setExpireSeconds(nextNode, userTaskBuilder, waitExecExpireIntervalSeconds);
            timerBuilder.addExecutionListener(ServiceTaskPendingExpiredListener.class);
        }, userTimerTaskBuilder -> {
            userTimerTaskBuilder.addExecutionListener(ServiceTaskPendingListener.class);
        });
    }

    public FlowInstanceConfigurer next(@NonNull FlowGatewayInstance nextNode) {
        return next(nextNode, (Consumer<BaseProcessNodeBuilder<? extends Gateway>>) gatewayBuilder -> gatewayBuilder
                .addExecutionListener(GatewayExecutingCompleteListener.class));
    }

    public FlowInstanceConfigurer route(@NonNull String expr, @NonNull FlowInstanceConfigurer configurer) {
        this.targetExecution.route(expr, configurer.getTargetExecution());
        BaseFlowNodeInstance to = configurer.first();
        if (to == null) {
            return this;
        }
        BaseFlowNodeInstance from = last();
        if (!(from instanceof FlowGatewayInstance)) {
            throw new IllegalStateException("Last node has to be a instance of FlowGatewayInstance");
        }
        return (FlowInstanceConfigurer) super.route(DEFAULT_EDGE_WEIGHT, configurer);
    }

    public FlowInstanceConfigurer route(@NonNull FlowInstanceConfigurer configurer) {
        this.targetExecution.route(configurer.getTargetExecution());
        BaseFlowNodeInstance to = configurer.first();
        if (to == null) {
            return this;
        }
        BaseFlowNodeInstance from = last();
        if (!(from instanceof FlowGatewayInstance)) {
            throw new IllegalStateException("Last node has to be a instance of FlowGatewayInstance");
        }
        return (FlowInstanceConfigurer) super.route(DEFAULT_EDGE_WEIGHT, configurer);
    }

    public FlowInstanceConfigurer endFlowInstance() {
        return endFlowInstance(null);
    }

    public FlowInstanceConfigurer endFlowInstance(Class<? extends BaseExecutionListener> endListenerClazz) {
        EndEventBuilder endEventBuilder = new EndEventBuilder();
        if (endListenerClazz != null) {
            endEventBuilder.addExecutionListener(endListenerClazz);
        }
        targetExecution.next(endEventBuilder);
        return this;
    }

    protected FlowInstanceConfigurer next(@NonNull FlowApprovalInstance nextNode,
            @NonNull Consumer<UserTaskBuilder> userTaskConsumer) {
        String userTaskName = FlowNodeType.APPROVAL_TASK.name() + "_user_task_" + nextNode.getId();
        UserTaskBuilder userTaskBuilder = nullSafeGetNodeBuilder(userTaskName, nextNode, () -> {
            UserTaskBuilder builder = new UserTaskBuilder(userTaskName);
            userTaskConsumer.accept(builder);
            return builder;
        });
        if (Objects.nonNull(nextNode.getExternalApprovalId())) {
            String serviceTaskName = FlowNodeType.APPROVAL_TASK.name() + "_external_approval_task_" + nextNode.getId();
            ServiceTaskBuilder serviceTaskBuilder = nullSafeGetNodeBuilder(serviceTaskName, nextNode,
                    () -> new ServiceTaskBuilder(serviceTaskName, CreateExternalApprovalTask.class));
            if (this.requiresActivityIdAndName) {
                flowableAdaptor.setFlowableElement(nextNode, new FlowableElement(serviceTaskBuilder));
            }
            String gatewayName = FlowNodeType.APPROVAL_TASK.name() + "_external_approval_gateway_" + nextNode.getId();
            ExclusiveGatewayBuilder gatewayBuilder = nullSafeGetNodeBuilder(gatewayName, nextNode,
                    () -> new ExclusiveGatewayBuilder(gatewayName));
            targetExecution.next(serviceTaskBuilder).next(gatewayBuilder);
            String expr = RuntimeTaskConstants.SUCCESS_CREATE_EXT_INS + "_" + nextNode.getId();
            targetExecution.route(String.format("${!%s}", expr), this.targetProcessBuilder.endProcess());
            targetExecution.next(userTaskBuilder, new ConditionSequenceFlowBuilder(
                    gatewayBuilder.getGraphId() + " -> " + serviceTaskBuilder.getGraphId(),
                    String.format("${%s}", expr)));
        } else {
            targetExecution.next(userTaskBuilder);
        }
        if (log.isDebugEnabled()) {
            log.debug(
                    "Successfully set up the approval task node instance, instanceId={}, instanceType={}, activityId={}, name={}",
                    nextNode.getId(), nextNode.getNodeType(), userTaskBuilder.getGraphId(), userTaskBuilder.getName());
        }
        return next(userTaskBuilder, nextNode);
    }

    protected FlowInstanceConfigurer next(@NonNull FlowTaskInstance nextNode,
            @NonNull Consumer<ServiceTaskBuilder> serviceTaskConsumer,
            @NonNull Consumer<UserTaskBuilder> userManuTaskConsumer,
            @NonNull Consumer<UserTaskBuilder> userTimerTaskConsumer) {
        Class<? extends BaseRuntimeFlowableDelegate<?>> clazz = nextNode.getTargetDelegateClass();
        Verify.notNull(clazz, "AbstractRuntimeFlowableDelegate.class");

        String serviceTaskName = FlowNodeType.SERVICE_TASK.name() + "_service_task_" + nextNode.getId();
        ServiceTaskBuilder serviceTaskBuilder = nullSafeGetNodeBuilder(serviceTaskName, nextNode, () -> {
            ServiceTaskBuilder taskBuilder = new ServiceTaskBuilder(serviceTaskName, clazz);
            serviceTaskConsumer.accept(taskBuilder);
            return taskBuilder;
        });

        ExecutionStrategyConfig strategyConfig = nextNode.getStrategyConfig();
        if (strategyConfig.getStrategy() == FlowTaskExecutionStrategy.AUTO) {
            targetExecution.next(serviceTaskBuilder);
            if (log.isDebugEnabled()) {
                log.debug("Set up the service task succeed, nodeInstanceId={}, activityId={}, name={}",
                        nextNode.getId(), serviceTaskBuilder.getGraphId(), serviceTaskBuilder.getName());
            }
            return next(serviceTaskBuilder, nextNode);
        }
        if (strategyConfig.getStrategy() == FlowTaskExecutionStrategy.TIMER) {
            if (log.isDebugEnabled()) {
                log.debug("Start defining a timer execution user task node instance, instanceId={}, instanceType={}, "
                        + "strategy={}", nextNode.getId(), nextNode.getTaskType(), strategyConfig.getStrategy());
            }
            String timerTaskName = FlowNodeType.SERVICE_TASK.name() + "_timer_task_" + nextNode.getId();
            UserTaskBuilder userTimerTaskBuilder = nullSafeGetNodeBuilder(timerTaskName, nextNode, () -> {
                UserTaskBuilder builder = new UserTaskBuilder(timerTaskName);
                TimerBoundaryEventBuilder timerBuilder =
                        setTimerBoundaryEvent(nextNode, builder, null, strategyConfig.getExecutionTime());
                timerBuilder.addExecutionListener(ServiceTaskPendingExpiredListener.class);
                targetProcessBuilder.newExecution(timerBuilder).next(serviceTaskBuilder);
                userTimerTaskConsumer.accept(builder);
                return builder;
            });
            if (this.requiresActivityIdAndName) {
                flowableAdaptor.setFlowableElement(nextNode, new FlowableElement(userTimerTaskBuilder));
            }
            targetExecution.next(userTimerTaskBuilder);
            if (log.isDebugEnabled()) {
                log.debug("Define a timer execution user task node instance completion, instanceId={}, intanceType={}, "
                        + "strategy={}", nextNode.getId(), nextNode.getNodeType(), strategyConfig.getStrategy());
            }
            String gatewayName = FlowNodeType.SERVICE_TASK.name() + "_timer_task_exclusive_gateway_" + nextNode.getId();
            ExclusiveGatewayBuilder gatewayBuilder =
                    nullSafeGetNodeBuilder(gatewayName, nextNode, () -> new ExclusiveGatewayBuilder(gatewayName));
            targetExecution.next(gatewayBuilder);
            if (log.isDebugEnabled()) {
                log.debug("Set up the gateway node succeed, instanceId={}, intanceType={}, activityId={}",
                        nextNode.getId(), nextNode.getNodeType(), gatewayBuilder.getGraphId());
            }
            targetExecution.route(String.format("${%s}", FlowTaskInstance.ABORT_VARIABLE_NAME),
                    this.targetProcessBuilder.endProcess());
            targetExecution.next(serviceTaskBuilder, new ConditionSequenceFlowBuilder(
                    gatewayBuilder.getGraphId() + " -> " + serviceTaskBuilder.getGraphId(),
                    String.format("${!%s}", FlowTaskInstance.ABORT_VARIABLE_NAME)));
            if (log.isDebugEnabled()) {
                log.debug("Set up the service task node succeed, nodeInstanceId={}, activityId={}, name={}",
                        nextNode.getId(), serviceTaskBuilder.getGraphId(), serviceTaskBuilder.getName());
            }
            return next(serviceTaskBuilder, nextNode);
        }

        if (log.isDebugEnabled()) {
            log.debug("Start defining a manual execution task node instance, instanceId={}, instanceType={}, "
                    + "strategy={}", nextNode.getId(), nextNode.getNodeType(), strategyConfig.getStrategy());
        }
        String waitTaskName = FlowNodeType.SERVICE_TASK.name() + "_wait_task_" + nextNode.getId();
        UserTaskBuilder userTaskBuilder = nullSafeGetNodeBuilder(waitTaskName, nextNode, () -> {
            UserTaskBuilder builder = new UserTaskBuilder(waitTaskName);
            userManuTaskConsumer.accept(builder);
            return builder;
        });
        if (this.requiresActivityIdAndName) {
            flowableAdaptor.setFlowableElement(nextNode, new FlowableElement(userTaskBuilder));
        }
        targetExecution.next(userTaskBuilder);
        if (log.isDebugEnabled()) {
            log.debug("Define a manual execution task node instance completion, instanceId={}, intanceType={}, "
                    + "strategy={}", nextNode.getId(), nextNode.getNodeType(), strategyConfig.getStrategy());
        }
        String gatewayName = FlowNodeType.SERVICE_TASK.name() + "_wait_task_exclusive_gateway_" + nextNode.getId();
        ExclusiveGatewayBuilder gatewayBuilder =
                nullSafeGetNodeBuilder(gatewayName, nextNode, () -> new ExclusiveGatewayBuilder(gatewayName));
        targetExecution.next(gatewayBuilder);
        if (log.isDebugEnabled()) {
            log.debug("Set up the gateway node succeed, instanceId={}, intanceType={}, activityId={}",
                    nextNode.getId(), nextNode.getNodeType(), gatewayBuilder.getGraphId());
        }
        targetExecution.route(String.format("${%s}", FlowTaskInstance.ABORT_VARIABLE_NAME),
                this.targetProcessBuilder.endProcess());
        targetExecution.next(serviceTaskBuilder, new ConditionSequenceFlowBuilder(
                gatewayBuilder.getGraphId() + " -> " + serviceTaskBuilder.getGraphId(),
                String.format("${!%s}", FlowTaskInstance.ABORT_VARIABLE_NAME)));
        if (log.isDebugEnabled()) {
            log.debug("Set up the service task node succeed, nodeInstanceId={}, activityId={}, name={}",
                    nextNode.getId(), serviceTaskBuilder.getGraphId(), serviceTaskBuilder.getName());
        }
        return next(serviceTaskBuilder, nextNode);
    }

    protected FlowInstanceConfigurer next(@NonNull FlowGatewayInstance nextNode,
            @NonNull Consumer<BaseProcessNodeBuilder<? extends Gateway>> gatewayConsumer) {
        String gatewayName = FlowNodeType.GATEWAY.name() + "_exclusive_gateway_" + nextNode.getId();
        ExclusiveGatewayBuilder gatewayBuilder = nullSafeGetNodeBuilder(gatewayName, nextNode, () -> {
            ExclusiveGatewayBuilder builder = new ExclusiveGatewayBuilder(gatewayName);
            gatewayConsumer.accept(builder);
            return builder;
        });
        targetExecution.next(gatewayBuilder);
        if (log.isDebugEnabled()) {
            log.debug("Set up the gateway node succeed, instanceId={}, intanceType={}, activityId={}, name={}",
                    nextNode.getId(), nextNode.getNodeType(), gatewayBuilder.getGraphId(), gatewayBuilder.getName());
        }
        return next(gatewayBuilder, nextNode);
    }

    protected <T extends Task> TimerBoundaryEventBuilder setExpireSeconds(
            @NonNull BaseFlowNodeInstance attachedNode,
            @NonNull BaseTaskBuilder<T> target, @NonNull Integer intervalSeconds) {
        TimerBoundaryEventBuilder timerBuilder = setTimerBoundaryEvent(attachedNode, target, intervalSeconds, null);
        targetProcessBuilder.newExecution(timerBuilder).endProcess();
        return timerBuilder;
    }

    private <T extends Task> TimerBoundaryEventBuilder setTimerBoundaryEvent(
            @NonNull BaseFlowNodeInstance attachedNode,
            @NonNull BaseTaskBuilder<T> target, Integer intervalSeconds, Date time) {
        Verify.verify(intervalSeconds != null || time != null, "Expire settings can not be null");
        Verify.verify(intervalSeconds == null || time == null, "Time and interval seconds can't both be set");
        if (log.isDebugEnabled()) {
            log.debug("Start defining the execution expire interval, instanceId={}, intanceType={}",
                    attachedNode.getId(), attachedNode.getNodeType());
        }
        List<BaseProcessNodeBuilder<?>> subNodeBuilders = target.getSubProcessNodeBuilders();
        Verify.verify(subNodeBuilders.isEmpty(), "SubProcessNodeBuilder is not empty");
        TimerBoundaryEventBuilder timerBuilder;
        if (time != null) {
            timerBuilder = target.addTimerEvent(time, true);
        } else {
            timerBuilder = target.addTimerEvent(Duration.ofSeconds(intervalSeconds), true);
        }
        if (this.requiresActivityIdAndName) {
            flowableAdaptor.setFlowableElement(attachedNode, new FlowableElement(timerBuilder));
        }
        if (log.isDebugEnabled()) {
            log.debug("Defining the execution expire interval completion, instanceId={}, intanceType={}",
                    attachedNode.getId(), attachedNode.getNodeType());
        }
        return timerBuilder;
    }

    protected ErrorBoundaryEventBuilder setHandleableError(@NonNull FlowTaskInstance nextNode,
            @NonNull ServiceTaskBuilder builder, @NonNull ErrorCode errorCode) {
        if (log.isDebugEnabled()) {
            log.debug("Start defining service task error handling logic, instanceId={}, intanceType={}, errorCode={}",
                    nextNode.getId(), nextNode.getNodeType(), errorCode);
        }
        List<BaseProcessNodeBuilder<?>> subNodeBuilders = builder.getSubProcessNodeBuilders();
        for (BaseProcessNodeBuilder<?> subNodeBuilder : subNodeBuilders) {
            if (!(subNodeBuilder instanceof ErrorBoundaryEventBuilder)) {
                continue;
            }
            ErrorBoundaryEventBuilder errBuilder = (ErrorBoundaryEventBuilder) subNodeBuilder;
            if (errBuilder.getErrorCode().equals(errorCode.code())) {
                return errBuilder;
            }
        }
        ErrorBoundaryEventBuilder errorBuilder = builder.addErrorProcessEvent(errorCode, true);
        if (this.requiresActivityIdAndName) {
            flowableAdaptor.setFlowableElement(nextNode, new FlowableElement(errorBuilder));
        }
        targetProcessBuilder.newExecution(errorBuilder).endProcess();
        if (log.isDebugEnabled()) {
            log.debug(
                    "Defining the service task error handling completion, instanceId={}, intanceType={}, errorCode={}",
                    nextNode.getId(), nextNode.getNodeType(), errorCode);
        }
        return errorBuilder;
    }

    protected FlowInstanceConfigurer next(@NonNull BaseProcessNodeBuilder<?> nodeBuilder,
            @NonNull BaseFlowNodeInstance nextNode) {
        BaseFlowNodeInstance from = last();
        if (from != null && from.isEndEndPoint()) {
            throw new IllegalStateException("Can not append node after EndEndPoint");
        }
        if (nextNode.getActivityId() == null) {
            nextNode.setActivityId(nodeBuilder.getGraphId());
        }
        if (nextNode.getName() == null) {
            nextNode.setName(nodeBuilder.getName());
        }
        return (FlowInstanceConfigurer) super.next(nextNode, DEFAULT_EDGE_WEIGHT);
    }

    @SuppressWarnings("all")
    protected <T extends BaseProcessNodeBuilder<? extends FlowNode>> T nullSafeGetNodeBuilder(@NonNull String name,
            @NonNull BaseFlowNodeInstance instance, @NonNull Supplier<T> supplier) {
        Optional<BaseProcessNodeBuilder<? extends FlowNode>> optional = accessor.getNodeBuilderByName(name, instance);
        if (optional.isPresent()) {
            return (T) optional.get();
        }
        T value = supplier.get();
        accessor.setNodeBuilder(instance, value);
        return value;
    }

}
