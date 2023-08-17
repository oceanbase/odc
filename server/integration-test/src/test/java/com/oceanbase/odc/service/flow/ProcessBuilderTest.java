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
package com.oceanbase.odc.service.flow;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.core.flow.builder.ErrorBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.ExclusiveGatewayBuilder;
import com.oceanbase.odc.core.flow.builder.FlowableModelBuilder;
import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.builder.ParallelGatewayBuilder;
import com.oceanbase.odc.core.flow.builder.ReceiveTaskBuilder;
import com.oceanbase.odc.core.flow.builder.ServiceTaskBuilder;
import com.oceanbase.odc.core.flow.builder.SignalCatchEventBuilder;
import com.oceanbase.odc.core.flow.builder.TimerBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.builder.UserTaskBuilder;
import com.oceanbase.odc.core.flow.model.FlowableFormType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.flow.tool.TestExecutionListener;
import com.oceanbase.odc.service.flow.tool.TestFlowableDelegateImpl;
import com.oceanbase.odc.service.flow.tool.TestUserTaskListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Test cases for Process Builder
 *
 * @author yh263208
 * @date 2022-01-27 11:26
 * @since ODC_release_3.3.0
 */
@Slf4j
public class ProcessBuilderTest extends ServiceTestEnv {

    private String receiveTaskId = null;
    private String signalEventId = null;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HistoryService historyService;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void buildProcess_withCycle_expThrown() {
        FlowableProcessBuilder builder = new FlowableProcessBuilder("Test Process");

        UserTaskBuilder userTaskBuilder1 = new UserTaskBuilder("second_route_u_task_1");
        UserTaskBuilder userTaskBuilder2 = new UserTaskBuilder("second_route_u_task_2");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Graph existence cycle");
        builder.newProcess().next(userTaskBuilder1).next(userTaskBuilder2).next(userTaskBuilder1).and().build();
    }

    @Test
    public void buildProcess_processContainsParallelGateway_goBothTwoRoutes() {
        String processInstanceId = startProcessInstance(getParallelGatewayProcess(), null);
        List<String> historyInstances = getHistoricActivityInstances(processInstanceId).stream()
                .map(HistoricActivityInstance::getActivityName).collect(Collectors.toList());

        List<String> secondRouteNameList = Arrays.asList("Start Node", "ParallelGateWay", "second_route_u_task_1",
                "second_route_u_task_2", "second_route_r_task_1", "ParallelGateWay1", "End Node");
        List<String> firstRouteNameList = Arrays.asList("Start Node", "ParallelGateWay", "first_route_u_task_1",
                "first_route_signal_event_1", "first_route_s_task_1", "first_route_u_task_2", "ParallelGateWay1",
                "End Node");

        Assert.assertTrue(historyInstances.containsAll(firstRouteNameList));
        Assert.assertTrue(historyInstances.containsAll(secondRouteNameList));
    }

    @Test
    public void buildProcess_processContainsExclusiveGateway_goOneRoute() {
        Map<String, Object> variables = new HashMap<>();
        variables.putIfAbsent("level", 1000);

        String processInstanceId = startProcessInstance(getExclusiveGatewayProcess(), variables);
        List<String> historyInstances = getHistoricActivityInstances(processInstanceId).stream().map(
                HistoricActivityInstance::getActivityName).collect(Collectors.toList());

        List<String> routeNameList = Arrays.asList("Start Node", "ExclusiveGateway", "second_route_u_task_1",
                "second_route_u_task_2", "second_route_r_task_1", "End Node");
        Assert.assertTrue(historyInstances.containsAll(routeNameList));
    }

    @Test
    public void buildProcess_processContainsExclusiveGateway_goAnotherRoute() {
        Map<String, Object> variables = new HashMap<>();
        variables.putIfAbsent("level", 1);

        String processInstanceId = startProcessInstance(getExclusiveGatewayProcess(), variables);
        List<String> historyInstances = getHistoricActivityInstances(processInstanceId).stream().map(
                HistoricActivityInstance::getActivityName).collect(Collectors.toList());

        List<String> routeNameList = Arrays.asList("Start Node", "ExclusiveGateway", "first_route_u_task_1",
                "first_route_signal_event_1", "first_route_s_task_1", "first_route_u_task_2", "End Node");
        Assert.assertTrue(historyInstances.containsAll(routeNameList));
    }

    private List<HistoricActivityInstance> getHistoricActivityInstances(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc().list();
    }

    private String startProcessInstance(Process process, Map<String, Object> variables) {
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);

        Deployment deployment = repositoryService.createDeployment().addBpmnModel("process.bpmn", bpmnModel).deploy();
        ProcessDefinition processDefinition =
                repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), variables);
        String pId = processInstance.getId();
        try {
            while (processInstance != null && !processInstance.isEnded()) {
                List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
                runtimeService.createExecutionQuery().processInstanceId(pId).list().forEach(e -> {
                    if (Objects.equals(receiveTaskId, e.getActivityId())) {
                        runtimeService.trigger(e.getId());
                    } else if (Objects.equals(signalEventId, e.getActivityId())) {
                        runtimeService.signalEventReceived("first_route_signal_1");
                    }
                });
                for (Task task : tasks) {
                    taskService.complete(task.getId());
                }
                processInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }
        } catch (Exception exception) {
            log.warn("Failed to run instance, instanceId={}", processInstance.getId(), exception);
            runtimeService.deleteProcessInstance(pId, exception.getMessage());
            throw exception;
        }
        return pId;
    }

    private Process getParallelGatewayProcess() {
        FlowableProcessBuilder builder = new FlowableProcessBuilder("Test Process");

        ParallelGatewayBuilder parallelGatewayBuilder = new ParallelGatewayBuilder("ParallelGateWay");
        ParallelGatewayBuilder parallelGatewayBuilder1 = new ParallelGatewayBuilder("ParallelGateWay1");

        ExecutionConfigurer first = firstExecution(builder);
        ExecutionConfigurer second = secondExecution(builder);
        ExecutionConfigurer end = builder.newExecution(parallelGatewayBuilder1).endProcess();

        return builder.newProcess()
                .next(parallelGatewayBuilder)
                .route(first)
                .route(second)
                .and()
                .converge(Arrays.asList(first, second), end)
                .build();
    }

    private Process getExclusiveGatewayProcess() {
        FlowableProcessBuilder builder = new FlowableProcessBuilder("Test Process");

        ExclusiveGatewayBuilder exclusiveGatewayBuilder = new ExclusiveGatewayBuilder("ExclusiveGateway");

        ExecutionConfigurer first = firstExecution(builder).endProcess(TestExecutionListener.class);
        ExecutionConfigurer second = secondExecution(builder).endProcess();

        return builder.newProcess()
                .next(exclusiveGatewayBuilder)
                .route("${level == 1}", first)
                .route(second)
                .and()
                .build();
    }

    private ExecutionConfigurer secondExecution(FlowableProcessBuilder builder) {
        UserTaskBuilder userTaskBuilder1 = new UserTaskBuilder("second_route_u_task_1");
        TimerBoundaryEventBuilder timerBuilder = userTaskBuilder1.addTimerEvent(Duration.ofSeconds(1), true);

        UserTaskBuilder userTaskBuilder2 = new UserTaskBuilder("second_route_u_task_2");
        ReceiveTaskBuilder receiveTaskBuilder = new ReceiveTaskBuilder("second_route_r_task_1");
        receiveTaskId = receiveTaskBuilder.getGraphId();

        ExecutionConfigurer configurer = builder.newExecution(userTaskBuilder1)
                .next(userTaskBuilder2)
                .next(receiveTaskBuilder);
        builder.newExecution(timerBuilder).next(receiveTaskBuilder);
        return configurer;
    }

    private ExecutionConfigurer firstExecution(FlowableProcessBuilder builder) {
        UserTaskBuilder userTaskBuilder = new UserTaskBuilder("first_route_u_task_1");
        userTaskBuilder.withAssignee("David")
                .addTaskListener(TestUserTaskListener.class)
                .addFormProperty(FlowableModelBuilder.getPropertyBuilder()
                        .withFormType(FlowableFormType.STRING_TYPE)
                        .withName("username").build())
                .addFormProperty(FlowableModelBuilder.getPropertyBuilder()
                        .withFormType(FlowableFormType.STRING_TYPE)
                        .withName("age").build())
                .addExecutionListener(TestExecutionListener.class);

        SignalCatchEventBuilder signalBuilder =
                new SignalCatchEventBuilder("first_route_signal_event_1", "first_route_signal_1");
        signalEventId = signalBuilder.getGraphId();

        ServiceTaskBuilder serviceTaskBuilder =
                new ServiceTaskBuilder("first_route_s_task_1", TestFlowableDelegateImpl.class);
        ErrorBoundaryEventBuilder errorBuilder =
                serviceTaskBuilder.addErrorProcessEvent(ErrorCodes.InternalServerError, true);

        UserTaskBuilder userTaskBuilder1 = new UserTaskBuilder("first_route_u_task_2");

        ExecutionConfigurer configurer = builder.newExecution(userTaskBuilder)
                .next(signalBuilder)
                .next(serviceTaskBuilder)
                .next(userTaskBuilder1);
        builder.newExecution(errorBuilder).next(userTaskBuilder1);
        return configurer;
    }

}
