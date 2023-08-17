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
package com.oceanbase.odc.service.onlineschemachange.pipeline;

/**
 * @author yaobin
 * @date 2023-06-10
 * @since 4.2.0
 */
public class DefaultLinkPipeline implements Pipeline {

    /**
     * first valve in pipeline
     */
    protected Valve first = null;
    /**
     * basic valve which be invoked latest
     */
    protected Valve basic = null;

    @Override
    public void setBasic(Valve valve) {
        Valve oldBasic = this.basic;
        Valve current = first;
        while (current != null) {
            if (current.getNext() == oldBasic) {
                current.setNext(valve);
                break;
            }
            current = current.getNext();
        }
        this.basic = valve;
    }

    public Valve getFirst() {
        if (first != null) {
            return first;
        }
        return basic;
    }

    @Override
    public void addValve(Valve valve) {
        if (first == null) {
            first = valve;
            valve.setNext(basic);
        } else {
            Valve current = first;
            while (current != null) {
                if (current.getNext() == basic) {
                    current.setNext(valve);
                    valve.setNext(basic);
                    break;
                }
                current = current.getNext();
            }
        }
    }

    @Override
    public void invoke(ValveContext context) {
        Valve valve = getFirst();
        if (valve != null) {
            valve.invoke(context);
            return;
        }
        throw new IllegalArgumentException("No valve in pipeline");
    }
}
