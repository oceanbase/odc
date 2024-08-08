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
package com.oceanbase.odc.service.onlineschemachange.fms;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.fsm.ActionContext;
import com.oceanbase.odc.service.onlineschemachange.fsm.ActionFsm;
import com.oceanbase.odc.service.onlineschemachange.fsm.StateTransfer;

/**
 * test for action fsm
 * 
 * @author longpeng.zlp
 * @date 2024/7/25 14:06
 * @since 4.3.1
 */
public class ActionFsmTest {

    @Test
    public void testActionFsm() {
        AtomicInteger rollbackCounter = new AtomicInteger(0);
        // init action fsm
        // expect 0 -> 1 -> 2 -> 2 -> 2 -> 2...
        // cause action 2 throws exception, and state will not moved
        TestActionFsm actionFsm = new TestActionFsm();
        actionFsm.registerEvent("0", new TextAction(false), new TestStateTransfer(), Collections.singleton("1"));
        actionFsm.registerEvent("1", new TextAction(false), new TestStateTransfer(), Collections.singleton("1"));
        actionFsm.registerEvent("2", new TextAction(true, (t) -> rollbackCounter.getAndIncrement()),
                new TestStateTransfer(), Collections.singleton("1"));
        actionFsm.registerEvent("3", new TextAction(false), new TestStateTransfer(), Collections.singleton("1"));
        TestContext testContext = new TestContext();
        testContext.prevVal = -1;
        testContext.currentVal = 0;
        // state 0 -> 1
        actionFsm.schedule(testContext);
        Assert.assertEquals(testContext.prevVal, 0);
        Assert.assertEquals(testContext.currentVal, 1);
        // state 1 -> 2
        actionFsm.schedule(testContext);
        Assert.assertEquals(testContext.prevVal, 1);
        Assert.assertEquals(testContext.currentVal, 2);
        // state 2 -> 2
        actionFsm.schedule(testContext);
        Assert.assertEquals(testContext.prevVal, 1);
        Assert.assertEquals(testContext.currentVal, 2);
        // state 2 -> 2
        actionFsm.schedule(testContext);
        Assert.assertEquals(testContext.prevVal, 1);
        Assert.assertEquals(testContext.currentVal, 2);
        // check rollback count
        Assert.assertEquals(rollbackCounter.get(), 2);
    }

    private static final class TestContext implements ActionContext {
        long currentVal;
        long prevVal;
    }

    private static final class TextAction implements Action<TestContext, Long> {
        final boolean shouldThrowException;
        final Consumer<TestContext> rollbackListener;

        private TextAction(boolean shouldThrowException) {
            this.shouldThrowException = shouldThrowException;
            this.rollbackListener = null;
        }

        private TextAction(boolean shouldThrowException, Consumer<TestContext> rollbackListener) {
            this.shouldThrowException = shouldThrowException;
            this.rollbackListener = rollbackListener;
        }

        @Override
        public Long execute(TestContext context) throws Exception {
            if (shouldThrowException) {
                throw new RuntimeException();
            }
            return context.currentVal + 1;
        }

        public void rollback(TestContext context) {
            rollbackListener.accept(context);
        }
    }

    private static final class TestStateTransfer implements StateTransfer<TestContext, Long> {
        @Override
        public String translateToNewState(String currentState, Long result, TestContext context) {
            return String.valueOf(result);
        }
    }

    private static final class TestActionFsm extends ActionFsm<TestContext, Long> {

        @Override
        public String resolveState(TestContext context) {
            return String.valueOf(context.currentVal);
        }

        @Override
        public void onActionComplete(String currentState, String nextState, String extraInfo, TestContext context) {
            context.prevVal = Long.valueOf(currentState);
            context.currentVal = Long.valueOf(nextState);
        }

        @Override
        public void handleException(TestContext context, Throwable e) {}
    }
}
