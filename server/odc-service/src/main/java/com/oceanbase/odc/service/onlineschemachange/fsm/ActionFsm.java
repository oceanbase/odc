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
package com.oceanbase.odc.service.onlineschemachange.fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * ActionFsm base impl
 * 
 * @param <Context> input context for execute
 * @param <ActionResult> function result of execute
 * @author longpeng.zlp
 * @date 2024/7/5 17:49
 * @since 4.3.1
 */
@Slf4j
public abstract class ActionFsm<Context extends ActionContext, ActionResult> {

    protected final Map<String, FSMNode<Context, ActionResult>> eventMap = new HashMap<>();

    protected ActionFsm() {}

    /**
     * resolve current from context
     * 
     * @param context context of FSM
     * @return
     */
    public abstract String resolveState(Context context);

    /**
     * when action is invoked
     * 
     * @param currentState current state has done
     * @param nextState next state to process
     * @param context context of FSM
     */
    public abstract void onActionComplete(String currentState, String nextState, String extraInfo, Context context);

    /**
     * handle exception
     * 
     * @param e exception thrown by ${@link Action#execute}
     */
    public abstract void handleException(Context context, Throwable e);

    /**
     * main loop trigger action execute and state change
     * 
     * @param context context of FSM
     * @return true if schedule success. false if exception found
     */
    public boolean schedule(Context context) {
        String state = resolveState(context);
        Preconditions.checkArgument(null != state, "state require not null");
        FSMNode<Context, ActionResult> fsmNode = eventMap.get(state);
        Preconditions.checkArgument(null != fsmNode, "state node not registered for state [" + state + "]");
        // do action
        ActionResult result = null;
        try {
            result = fsmNode.event.execute(context);
        } catch (Throwable e) {
            fsmNode.event.rollback(context);
            log.warn("Action state change meet exception, current {}, expected {}, exception {}", state,
                    fsmNode.expectTranslateStates, e);
            handleException(context, e);
            return false;
        }
        // translate state
        String nextState = fsmNode.stateTransfer.translateToNewState(state, result, context);
        if (!fsmNode.expectTranslateStates.contains(nextState)) {
            log.warn("Action state change meet unexpected state, current {}, expected {}, actual {}", state,
                    fsmNode.expectTranslateStates, nextState);
        }
        // notify state change
        onActionComplete(state, nextState, JsonUtils.toJson(result), context);
        return true;
    }

    protected Logger getLogger() {
        return log;
    }

    /**
     * build event related map
     * 
     * @param state current state
     * @param eventAction action for current state
     * @param stateTransfer state transfer to generate next state
     * @param expectTranslateStates next states expected to transfer
     */
    public void registerEvent(String state, Action<Context, ActionResult> eventAction,
            StateTransfer<Context, ActionResult> stateTransfer, Set<String> expectTranslateStates) {
        eventMap.put(state, new FSMNode<>(state, expectTranslateStates, eventAction, stateTransfer));
    }

    private static class FSMNode<Context extends ActionContext, K> {
        final String currentState;
        final Set<String> expectTranslateStates;
        final Action<Context, K> event;
        final StateTransfer<Context, K> stateTransfer;

        private FSMNode(String currentState, Set<String> expectTranslateStatesSet, Action<Context, K> event,
                StateTransfer<Context, K> stateTransfer) {
            this.currentState = currentState;
            this.stateTransfer = stateTransfer;
            this.expectTranslateStates = new HashSet<>();
            if (null != expectTranslateStatesSet) {
                this.expectTranslateStates.addAll(expectTranslateStatesSet);
            }
            this.event = event;
        }
    }
}

