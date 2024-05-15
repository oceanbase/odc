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

import org.apache.commons.lang.Validate;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.Task;

import com.oceanbase.odc.common.graph.GraphConfigurer;
import com.oceanbase.odc.core.flow.BaseExecutionListener;
import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.core.flow.builder.BaseProcessNodeBuilder;
import com.oceanbase.odc.core.flow.builder.BaseTaskBuilder;
import com.oceanbase.odc.core.flow.builder.ConditionSequenceFlowBuilder;
import com.oceanbase.odc.core.flow.builder.EndEventBuilder;
import com.oceanbase.odc.core.flow.builder.ErrorBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.ExclusiveGatewayBuilder;
import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.builder.SequenceFlowBuilder;
import com.oceanbase.odc.core.flow.builder.ServiceTaskBuilder;
import com.oceanbase.odc.core.flow.builder.TimerBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.UserTaskBuilder;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.listener.ApprovalStatusNotifyListener;
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
import com.oceanbase.odc.service.flow.task.CreateExternalApprovalTask;
import com.oceanbase.odc.service.flow.task.FlowTaskSubmitter;
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

    private static final float DEFAULT_EDGE_WEIGHT = 1;
    /**
     * 在实际的配置过程中，可能会出现多次操作同一个流程节点（例如多次 next 同一节点），原理上要求这种情况下拓扑图中链接到的是同一个对象，这种场景下就必须通过accessor
     * 根据名字获取到同一个对象才行。
     */
    private final ProcessNodeBuilderAccessor accessor;
    /**
     * Built-in {@link FlowableProcessBuilder} of {@link FlowInstance}
     */
    private final FlowableProcessBuilder targetProcessBuilder;
    private final Long flowInstanceId;
    protected final FlowableAdaptor flowableAdaptor;
    /**
     * Current {@link ExecutionConfigurer} of target {@link FlowableProcessBuilder}
     */
    protected final ExecutionConfigurer targetExecution;

    /**
     * This constructor can not be invoked by user
     *
     * @param flowInstance target {@link FlowInstance}
     */
    protected FlowInstanceConfigurer(@NonNull FlowInstance flowInstance,
            @NonNull FlowableProcessBuilder targetProcessBuilder, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull ProcessNodeBuilderAccessor accessor) {
        super(flowInstance);
        this.targetProcessBuilder = targetProcessBuilder;
        this.flowableAdaptor = flowableAdaptor;
        this.accessor = accessor;
        Validate.notNull(flowInstance.getId(), "FlowInstanceId can not be null");
        this.flowInstanceId = flowInstance.getId();
        this.targetExecution = targetProcessBuilder.newExecution();
    }

    /**
     * This constructor can not be invoked by user
     *
     * @param flowInstance target {@link FlowInstance}
     */
    protected FlowInstanceConfigurer(@NonNull FlowInstance flowInstance,
            @NonNull FlowableProcessBuilder targetProcessBuilder, @NonNull ExecutionConfigurer targetExecution,
            @NonNull FlowableAdaptor flowableAdaptor, @NonNull ProcessNodeBuilderAccessor accessor) {
        super(flowInstance);
        this.accessor = accessor;
        this.targetExecution = targetExecution;
        this.targetProcessBuilder = targetProcessBuilder;
        this.flowableAdaptor = flowableAdaptor;
        Validate.notNull(flowInstance.getId(), "FlowInstanceId can not be null");
        this.flowInstanceId = flowInstance.getId();
    }

    public FlowInstanceConfigurer next(@NonNull FlowApprovalInstance nextNode) {
        return next(nextNode, (Consumer<UserTaskBuilder>) userTaskBuilder -> {
            userTaskBuilder.addTaskListener(BaseTaskBindUserTaskListener.class);
            userTaskBuilder.addExecutionListener(BaseTaskExecutingCompleteListener.class);
            userTaskBuilder.addExecutionListener(ApprovalStatusNotifyListener.class);
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
        String userTaskName = FlowNodeType.APPROVAL_TASK.name() + "_user_task_" + getNameSuffix(nextNode);
        UserTaskBuilder userTaskBuilder = nullSafeGetNodeBuilder(userTaskName, nextNode, () -> {
            UserTaskBuilder builder = new UserTaskBuilder(userTaskName);
            userTaskConsumer.accept(builder);
            return builder;
        });
        if (Objects.nonNull(nextNode.getExternalApprovalId())) {
            String serviceTaskName = FlowNodeType.APPROVAL_TASK.name()
                    + "_external_approval_task_" + getNameSuffix(nextNode);
            ServiceTaskBuilder serviceTaskBuilder = nullSafeGetNodeBuilder(serviceTaskName, nextNode,
                    () -> new ServiceTaskBuilder(serviceTaskName, CreateExternalApprovalTask.class));
            nextNode.bindFlowableElement(new FlowableElement(serviceTaskBuilder));
            String gatewayName = FlowNodeType.APPROVAL_TASK.name()
                    + "_external_approval_gateway_" + getNameSuffix(nextNode);
            ExclusiveGatewayBuilder gatewayBuilder = nullSafeGetNodeBuilder(gatewayName, nextNode,
                    () -> new ExclusiveGatewayBuilder(gatewayName));
            targetExecution.next(serviceTaskBuilder).next(gatewayBuilder);
            targetExecution.route(String.format("${!%s}", RuntimeTaskConstants.SUCCESS_CREATE_EXT_INS),
                    this.targetProcessBuilder.endProcess());
            targetExecution.next(userTaskBuilder, new ConditionSequenceFlowBuilder(
                    gatewayBuilder.getGraphId() + " -> " + serviceTaskBuilder.getGraphId(),
                    String.format("${%s}", RuntimeTaskConstants.SUCCESS_CREATE_EXT_INS)));
        } else {
            targetExecution.next(userTaskBuilder);
        }
        if (log.isDebugEnabled()) {
            log.debug("Successfully set up the approval task node instance, instanceType={}, activityId={}, name={}",
                    nextNode.getNodeType(), userTaskBuilder.getGraphId(), userTaskBuilder.getName());
        }
        return next(userTaskBuilder, nextNode);
    }

    protected FlowInstanceConfigurer next(@NonNull FlowTaskInstance nextNode,
            @NonNull Consumer<ServiceTaskBuilder> serviceTaskConsumer,
            @NonNull Consumer<UserTaskBuilder> userManuTaskConsumer,
            @NonNull Consumer<UserTaskBuilder> userTimerTaskConsumer) {

        FlowInstanceConfigurer configurer = nextInternal(nextNode, serviceTaskConsumer,
                userManuTaskConsumer, userTimerTaskConsumer);
        String userTaskName =
                FlowNodeType.APPROVAL_TASK.name() + RuntimeTaskConstants.CALLBACK_TASK + getNameSuffix(nextNode);
        UserTaskBuilder userTaskBuilder = nullSafeGetNodeBuilder(userTaskName, nextNode, () -> {
            UserTaskBuilder utb = new UserTaskBuilder(userTaskName);
            return utb;
        });
        targetExecution.next(userTaskBuilder);
        nextNode.bindFlowableElement(new FlowableElement(userTaskBuilder));
        String gatewayName = FlowNodeType.APPROVAL_TASK.name()
                + "_callback_gateway_" + getNameSuffix(nextNode);
        ExclusiveGatewayBuilder gatewayBuilder = nullSafeGetNodeBuilder(gatewayName, nextNode,
                () -> new ExclusiveGatewayBuilder(gatewayName));
        targetExecution.next(gatewayBuilder);

        // save as next GraphEdge
        SequenceFlowBuilder sequenceFlowBuilder = new ConditionSequenceFlowBuilder(
                gatewayBuilder.getGraphId() + " -> " + "_task_execute_succeed_",
                String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME));

        targetExecution.setPreviousGraphEdge(sequenceFlowBuilder);
        targetExecution.route(String.format("${!%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME),
                this.targetProcessBuilder.endProcess());
        return configurer;
    }

    protected FlowInstanceConfigurer nextInternal(@NonNull FlowTaskInstance nextNode,
            @NonNull Consumer<ServiceTaskBuilder> serviceTaskConsumer,
            @NonNull Consumer<UserTaskBuilder> userManuTaskConsumer,
            @NonNull Consumer<UserTaskBuilder> userTimerTaskConsumer) {

        String serviceTaskName = FlowNodeType.SERVICE_TASK.name() + "_service_task_" + getNameSuffix(nextNode);
        ServiceTaskBuilder serviceTaskBuilder = nullSafeGetNodeBuilder(serviceTaskName, nextNode, () -> {
            ServiceTaskBuilder taskBuilder = new ServiceTaskBuilder(serviceTaskName, FlowTaskSubmitter.class);
            serviceTaskConsumer.accept(taskBuilder);
            return taskBuilder;
        });

        ExecutionStrategyConfig strategyConfig = nextNode.getStrategyConfig();
        if (strategyConfig.getStrategy() == FlowTaskExecutionStrategy.AUTO) {
            targetExecution.next(serviceTaskBuilder);
            if (log.isDebugEnabled()) {
                log.debug("Set up the service task succeed, activityId={}, name={}",
                        serviceTaskBuilder.getGraphId(), serviceTaskBuilder.getName());
            }
            return next(serviceTaskBuilder, nextNode);
        }
        if (strategyConfig.getStrategy() == FlowTaskExecutionStrategy.TIMER) {
            if (log.isDebugEnabled()) {
                log.debug("Start defining a timer execution user task node instance, instanceType={}, strategy={}",
                        nextNode.getTaskType(), strategyConfig.getStrategy());
            }
            String timerTaskName = FlowNodeType.SERVICE_TASK.name() + "_timer_task_" + getNameSuffix(nextNode);
            UserTaskBuilder userTimerTaskBuilder = nullSafeGetNodeBuilder(timerTaskName, nextNode, () -> {
                UserTaskBuilder builder = new UserTaskBuilder(timerTaskName);
                TimerBoundaryEventBuilder timerBuilder =
                        setTimerBoundaryEvent(nextNode, builder, null, strategyConfig.getExecutionTime());
                timerBuilder.addExecutionListener(ServiceTaskPendingExpiredListener.class);
                targetProcessBuilder.newExecution(timerBuilder).next(serviceTaskBuilder);
                userTimerTaskConsumer.accept(builder);
                return builder;
            });
            nextNode.bindFlowableElement(new FlowableElement(userTimerTaskBuilder));
            targetExecution.next(userTimerTaskBuilder);
            if (log.isDebugEnabled()) {
                log.debug("Define a timer execution user task node instance completion, intanceType={}, strategy={}",
                        nextNode.getNodeType(), strategyConfig.getStrategy());
            }
            String gatewayName = FlowNodeType.SERVICE_TASK.name()
                    + "_timer_task_exclusive_gateway_" + getNameSuffix(nextNode);
            ExclusiveGatewayBuilder gatewayBuilder =
                    nullSafeGetNodeBuilder(gatewayName, nextNode, () -> new ExclusiveGatewayBuilder(gatewayName));
            targetExecution.next(gatewayBuilder);
            if (log.isDebugEnabled()) {
                log.debug("Set up the gateway node succeed, intanceType={}, activityId={}",
                        nextNode.getNodeType(), gatewayBuilder.getGraphId());
            }
            targetExecution.route(String.format("${%s}", FlowTaskInstance.ABORT_VARIABLE_NAME),
                    this.targetProcessBuilder.endProcess());
            targetExecution.next(serviceTaskBuilder, new ConditionSequenceFlowBuilder(
                    gatewayBuilder.getGraphId() + " -> " + serviceTaskBuilder.getGraphId(),
                    String.format("${!%s}", FlowTaskInstance.ABORT_VARIABLE_NAME)));
            if (log.isDebugEnabled()) {
                log.debug("Set up the service task node succeed, activityId={}, name={}",
                        serviceTaskBuilder.getGraphId(), serviceTaskBuilder.getName());
            }
            return next(serviceTaskBuilder, nextNode);
        }

        if (log.isDebugEnabled()) {
            log.debug("Start defining a manual execution task node instance, instanceType={}, strategy={}",
                    nextNode.getNodeType(), strategyConfig.getStrategy());
        }
        String waitTaskName = FlowNodeType.SERVICE_TASK.name() + "_wait_task_" + getNameSuffix(nextNode);
        UserTaskBuilder userTaskBuilder = nullSafeGetNodeBuilder(waitTaskName, nextNode, () -> {
            UserTaskBuilder builder = new UserTaskBuilder(waitTaskName);
            userManuTaskConsumer.accept(builder);
            return builder;
        });
        nextNode.bindFlowableElement(new FlowableElement(userTaskBuilder));
        targetExecution.next(userTaskBuilder);
        if (log.isDebugEnabled()) {
            log.debug("Define a manual execution task node instance completion, intanceType={}, strategy={}",
                    nextNode.getNodeType(), strategyConfig.getStrategy());
        }
        String gatewayName = FlowNodeType.SERVICE_TASK.name()
                + "_wait_task_exclusive_gateway_" + getNameSuffix(nextNode);
        ExclusiveGatewayBuilder gatewayBuilder =
                nullSafeGetNodeBuilder(gatewayName, nextNode, () -> new ExclusiveGatewayBuilder(gatewayName));
        targetExecution.next(gatewayBuilder);
        if (log.isDebugEnabled()) {
            log.debug("Set up the gateway node succeed, intanceType={}, activityId={}",
                    nextNode.getNodeType(), gatewayBuilder.getGraphId());
        }
        targetExecution.route(String.format("${%s}", FlowTaskInstance.ABORT_VARIABLE_NAME),
                this.targetProcessBuilder.endProcess());
        targetExecution.next(serviceTaskBuilder, new ConditionSequenceFlowBuilder(
                gatewayBuilder.getGraphId() + " -> " + serviceTaskBuilder.getGraphId(),
                String.format("${!%s}", FlowTaskInstance.ABORT_VARIABLE_NAME)));
        if (log.isDebugEnabled()) {
            log.debug("Set up the service task node succeed, activityId={}, name={}",
                    serviceTaskBuilder.getGraphId(), serviceTaskBuilder.getName());
        }
        return next(serviceTaskBuilder, nextNode);
    }

    protected FlowInstanceConfigurer next(@NonNull FlowGatewayInstance nextNode,
            @NonNull Consumer<BaseProcessNodeBuilder<? extends Gateway>> gatewayConsumer) {
        String gatewayName = FlowNodeType.GATEWAY.name() + "_exclusive_gateway_" + getNameSuffix(nextNode);
        ExclusiveGatewayBuilder gatewayBuilder = nullSafeGetNodeBuilder(gatewayName, nextNode, () -> {
            ExclusiveGatewayBuilder builder = new ExclusiveGatewayBuilder(gatewayName);
            gatewayConsumer.accept(builder);
            return builder;
        });
        targetExecution.next(gatewayBuilder);
        if (log.isDebugEnabled()) {
            log.debug("Set up the gateway node succeed, intanceType={}, activityId={}, name={}",
                    nextNode.getNodeType(), gatewayBuilder.getGraphId(), gatewayBuilder.getName());
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

    private String getNameSuffix(BaseFlowNodeInstance inst) {
        return this.flowInstanceId + "-" + inst.getShortUniqueId();
    }

    private <T extends Task> TimerBoundaryEventBuilder setTimerBoundaryEvent(
            @NonNull BaseFlowNodeInstance attachedNode,
            @NonNull BaseTaskBuilder<T> target, Integer intervalSeconds, Date time) {
        Verify.verify(intervalSeconds != null || time != null, "Expire settings can not be null");
        Verify.verify(intervalSeconds == null || time == null, "Time and interval seconds can't both be set");
        if (log.isDebugEnabled()) {
            log.debug("Start defining the execution expire interval, intanceType={}", attachedNode.getNodeType());
        }
        List<BaseProcessNodeBuilder<?>> subNodeBuilders = target.getSubProcessNodeBuilders();
        Verify.verify(subNodeBuilders.isEmpty(), "SubProcessNodeBuilder is not empty");
        TimerBoundaryEventBuilder timerBuilder;
        if (time != null) {
            timerBuilder = target.addTimerEvent(time, true);
        } else {
            timerBuilder = target.addTimerEvent(Duration.ofSeconds(intervalSeconds), true);
        }
        attachedNode.bindFlowableElement(new FlowableElement(timerBuilder));
        if (log.isDebugEnabled()) {
            log.debug("Defining the execution expire interval completion, intanceType={}", attachedNode.getNodeType());
        }
        return timerBuilder;
    }

    protected ErrorBoundaryEventBuilder setHandleableError(@NonNull FlowTaskInstance nextNode,
            @NonNull ServiceTaskBuilder builder, @NonNull ErrorCode errorCode) {
        if (log.isDebugEnabled()) {
            log.debug("Start defining service task error handling logic, intanceType={}, errorCode={}",
                    nextNode.getNodeType(), errorCode);
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
        nextNode.bindFlowableElement(new FlowableElement(errorBuilder));
        targetProcessBuilder.newExecution(errorBuilder).endProcess();
        if (log.isDebugEnabled()) {
            log.debug("Defining the service task error handling completion, intanceType={}, errorCode={}",
                    nextNode.getNodeType(), errorCode);
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
