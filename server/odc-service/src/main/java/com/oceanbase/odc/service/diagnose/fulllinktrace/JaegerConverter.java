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

package com.oceanbase.odc.service.diagnose.fulllinktrace;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.model.TraceSpan;

import lombok.Builder;
import lombok.Getter;

public class JaegerConverter implements ThirdPartyTraceConverter {
    private static final String PROCESS_ID_FORMAT = "%s-%s:%s";

    @Override
    public String convert(TraceSpan span) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("traceID", span.getLogTraceId());
        List<Span> spans = new ArrayList<>();
        Map<String, Process> processes = new HashMap<>();
        dataMap.put("spans", spans);
        dataMap.put("processes", processes);

        dfs(spans, processes, span, null);

        Map<String, Object> root = new HashMap<>();
        root.put("data", Collections.singletonList(dataMap));
        return JsonUtils.prettyToJson(root);
    }

    private void dfs(List<Span> spans, Map<String, Process> processes, TraceSpan spanNode, TraceSpan parent) {
        if (spanNode == null) {
            return;
        }

        String processId =
                String.format(PROCESS_ID_FORMAT, spanNode.getNode(), spanNode.getHost(), spanNode.getPort());
        processes.putIfAbsent(processId,
                new Process(spanNode.getNode().name(),
                        Arrays.asList(new Tag("rec_svr_ip", spanNode.getHost()),
                                new Tag("rec_svr_port", spanNode.getPort() + ""),
                                new Tag("tenant_id", spanNode.getTenantId() + ""))));

        spans.add(Span.builder()
                .traceID(spanNode.getTraceId())
                .spanID(spanNode.getSpanId())
                .operationName(spanNode.getSpanName())
                .startTime(toTimestampWithMicros(spanNode.getStartTimestamp()))
                .duration(spanNode.getElapseMicroSeconds())
                .references(parent == null ? null
                        : Collections.singletonList(new Reference(parent.getTraceId(), parent.getSpanId())))
                .tags(CollectionUtils.isEmpty(spanNode.getTags()) ? null
                        : spanNode.getTags()
                                .stream().map(map -> {
                                    Entry<String, Object> entry = map.entrySet().iterator().next();
                                    return new Tag(entry.getKey(), entry.getValue().toString());
                                }).collect(Collectors.toList()))
                .processID(processId)
                .build());
        if (CollectionUtils.isEmpty(spanNode.getSubSpans())) {
            return;
        }
        for (TraceSpan child : spanNode.getSubSpans()) {
            dfs(spans, processes, child, spanNode);
        }
    }

    private long toTimestampWithMicros(String timeStr) {
        Instant instant = LocalDateTime.parse(timeStr, TraceSpan.TIMESTAMP_FORMATTER)
                .atZone(ZoneId.systemDefault()).toInstant();
        return instant.getEpochSecond() * 1000000 + instant.getNano() / 1000;
    }

    @Getter
    @Builder
    private static class Span {
        private String traceID;
        private String spanID;
        private String operationName;
        private List<Reference> references;
        private Long startTime;
        private Long duration;
        private List<Tag> tags;
        private String processID;
        @Builder.Default
        private List<Map<String, Object>> logs = null;
        @Builder.Default
        private List<Map<String, Object>> warnings = null;
    }

    @Getter
    private static class Process {
        String serviceName;
        List<Tag> tags;

        public Process(String serviceName, List<Tag> tags) {
            this.serviceName = serviceName;
            this.tags = tags;
        }
    }

    @Getter
    private static class Tag {
        String key;
        String type = "string";
        String value;

        public Tag(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @Getter
    private static class Reference {
        String refType = "CHILD_OF";
        String traceID;
        String spanID;

        public Reference(String traceID, String spanID) {
            this.traceID = traceID;
            this.spanID = spanID;
        }
    }

}
