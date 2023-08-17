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
package com.oceanbase.odc.core.flow.builder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.UserTask;

import com.oceanbase.odc.core.flow.BaseTaskListener;
import com.oceanbase.odc.core.flow.util.FlowConstants;

import lombok.NonNull;

/**
 * Refer to {@link UserTask}
 *
 * @author yh263208
 * @date 2022-01-19 16:54
 * @since ODC_release_3.3.0
 * @see BaseProcessNodeBuilder
 */
public class UserTaskBuilder extends BaseTaskBuilder<UserTask> {

    private String assignee = null;
    private String owner = null;
    private final List<String> candidateUsers = new LinkedList<>();
    private final List<String> candidateGroups = new LinkedList<>();
    private final Set<String> taskListenerClassNames = new HashSet<>();

    public UserTaskBuilder(@NonNull String name) {
        super(name);
    }

    public UserTaskBuilder withOwner(@NonNull String owner) {
        this.owner = owner;
        return this;
    }

    public UserTaskBuilder withAssignee(@NonNull String assignee) {
        this.assignee = assignee;
        return this;
    }

    public UserTaskBuilder addCandidateUser(@NonNull String username) {
        this.candidateUsers.add(username);
        return this;
    }

    public UserTaskBuilder addCandidateGroup(@NonNull String groupName) {
        this.candidateGroups.add(groupName);
        return this;
    }

    public UserTaskBuilder addTaskListener(@NonNull Class<? extends BaseTaskListener> listenerClass) {
        this.taskListenerClassNames.add(listenerClass.getName());
        return this;
    }

    protected void enableTaskListeners(@NonNull UserTask target) {
        List<FlowableListener> listeners = new LinkedList<>();
        String[] eventNames = new String[] {
                FlowConstants.TASK_COMPLETE_EVENT_NAME,
                FlowConstants.TASK_CREATE_EVENT_NAME,
                FlowConstants.TASK_DELETE_EVENT_NAME,
                FlowConstants.TASK_ASSIGN_EVENT_NAME
        };
        for (String listenerClass : taskListenerClassNames) {
            for (String eventName : eventNames) {
                FlowableListener executionListener = new FlowableListener();
                executionListener.setImplementation(listenerClass);
                executionListener.setImplementationType("class");
                executionListener.setEvent(eventName);
                listeners.add(executionListener);
            }
        }
        target.setTaskListeners(listeners);
    }

    @Override
    public UserTask build() {
        UserTask returnVal = new UserTask();
        init(returnVal);
        return returnVal;
    }

    @Override
    protected void init(@NonNull UserTask userTask) {
        super.init(userTask);
        List<FormProperty> properties = new LinkedList<>();
        forEachFormProperty(properties::add);
        if (!properties.isEmpty()) {
            userTask.setFormProperties(properties);
        }
        enableTaskListeners(userTask);
        if (owner != null) {
            userTask.setOwner(owner);
        }
        if (assignee != null) {
            userTask.setAssignee(assignee);
        }
        if (!candidateUsers.isEmpty()) {
            userTask.setCandidateUsers(candidateUsers);
        }
        if (!candidateGroups.isEmpty()) {
            userTask.setCandidateGroups(candidateGroups);
        }
    }

}
