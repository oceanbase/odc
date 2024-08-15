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

/**
 * Action that do real job in finite state machine
 * 
 * @param <Context> input context for execute
 * @param <ActionResult> function result of execute
 * @author longpeng.zlp
 * @date 2024/7/5 17:24
 * @since 4.3.1
 */
public interface Action<Context extends ActionContext, ActionResult> {
    /**
     * execute action with context
     * 
     * @param context context of FSM
     * @return result to determinate next state
     */
    ActionResult execute(Context context) throws Exception;

    /**
     * rollback current operation if needed
     * 
     * @param context context of FSM
     */
    default void rollback(Context context) {}
}
