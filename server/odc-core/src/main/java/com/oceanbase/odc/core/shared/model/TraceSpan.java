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
package com.oceanbase.odc.core.shared.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;
import lombok.NonNull;

@Data
public class TraceSpan {
    // The format of start_ts/end_ts are different between OceanBase versions.
    @JsonIgnore
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendOptional(DateTimeFormatter.ofPattern("d-MMM-yy hh.mm.ss.SSSSSS a"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
            .toFormatter()
            .withLocale(Locale.ENGLISH);
    public static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @JsonAlias("logs")
    private String logs;
    @JsonAlias("tags")
    private List<Map<String, Object>> tags;
    @JsonAlias("elapse")
    private Long elapseMicroSeconds;
    @JsonAlias("parent")
    private String parent;
    @JsonAlias("span_id")
    private String spanId;
    @JsonAlias("trace_id")
    private String traceId;
    @JsonAlias("span_name")
    private String spanName;
    @JsonAlias("tenant_id")
    private Integer tenantId;
    @JsonAlias("rec_svr_ip")
    private String host;
    @JsonAlias("rec_svr_port")
    private Integer port;
    private String startTimestamp;
    private String endTimestamp;
    // for internal usage
    private String logTraceId;
    private Node node;
    private List<TraceSpan> subSpans = new ArrayList<>();

    @JsonSetter("start_ts")
    public void setStartTs(String ts) {
        LocalDateTime ldt = LocalDateTime.parse(ts, TIMESTAMP_FORMATTER);
        this.startTimestamp = ldt.format(OUTPUT_FORMATTER);
    }

    @JsonSetter("end_ts")
    public void setEndTs(String ts) {
        LocalDateTime ldt = LocalDateTime.parse(ts, TIMESTAMP_FORMATTER);
        this.endTimestamp = ldt.format(OUTPUT_FORMATTER);
    }

    public void setTags(List<Object> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        List<Map<String, Object>> retList = new ArrayList<>();
        for (Object tag : tags) {
            parseTags(tag, retList);
        }
        this.tags = retList;
    }

    private void parseTags(Object tags, List<Map<String, Object>> retList) {
        if (tags instanceof Map) {
            retList.add((Map<String, Object>) tags);
        } else if (tags instanceof Collection) {
            for (Object tag : (Collection) tags) {
                parseTags(tag, retList);
            }
        }
    }

    public enum Node {
        JDBC,
        OBProxy,
        OBServer;

        public static Node from(@NonNull String spanName) {
            if (spanName.contains("oceanbase_jdbc")) {
                return JDBC;
            } else if (spanName.contains("ob_proxy")) {
                return OBProxy;
            }
            return OBServer;
        }
    }

}
