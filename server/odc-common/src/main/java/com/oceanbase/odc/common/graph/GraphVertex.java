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
package com.oceanbase.odc.common.graph;

import java.util.LinkedList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Node of the graph
 *
 * @author yh263208
 * @date 2022-01-14 16:49
 * @since ODC_release_3.3.0
 */
@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"inEdges", "outEdges"})
public class GraphVertex extends BaseGraphElement {
    @Setter
    private String label;
    private final List<GraphEdge> inEdges = new LinkedList<>();
    private final List<GraphEdge> outEdges = new LinkedList<>();

    public GraphVertex(@NonNull String id, String name) {
        super(id, name);
    }

    public boolean addInEdge(@NonNull GraphEdge inEdge) {
        if (inEdges.contains(inEdge)) {
            return false;
        }
        return this.inEdges.add(inEdge);
    }

    public boolean addOutEdge(@NonNull GraphEdge outEdge) {
        if (outEdges.contains(outEdge)) {
            return false;
        }
        return this.outEdges.add(outEdge);
    }

    public boolean removeOutEdge(@NonNull GraphEdge outEdge) {
        return this.outEdges.remove(outEdge);
    }

    public boolean removeInEdge(@NonNull GraphEdge inEdge) {
        return this.inEdges.remove(inEdge);
    }

}
