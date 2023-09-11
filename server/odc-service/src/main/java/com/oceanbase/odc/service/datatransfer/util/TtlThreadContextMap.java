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
package com.oceanbase.odc.service.datatransfer.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.spi.ThreadContextMap;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * @author xien.sxe
 * @date 2022/8/8 10:16
 * @since 1.0.0-SNAPSHOT
 */
public class TtlThreadContextMap implements ThreadContextMap {

    /**
     *
     */
    private final ThreadLocal<Map<String, String>> tl;

    /**
     *
     */
    public TtlThreadContextMap() {
        this.tl = new TransmittableThreadLocal<Map<String, String>>();
    }

    @Override
    public void put(final String key, final String value) {
        Map<String, String> map = this.tl.get();
        map = map == null ? new HashMap<>() : new HashMap<>(map);
        map.put(key, value);
        this.tl.set(Collections.unmodifiableMap(map));
    }

    @Override
    public String get(final String key) {
        final Map<String, String> map = this.tl.get();
        return map == null ? null : map.get(key);
    }

    @Override
    public void remove(final String key) {
        final Map<String, String> map = this.tl.get();
        if (map != null) {
            final Map<String, String> copy = new HashMap<>(map);
            copy.remove(key);
            this.tl.set(Collections.unmodifiableMap(copy));
        }
    }

    @Override
    public void clear() {
        this.tl.remove();
    }

    @Override
    public boolean containsKey(final String key) {
        final Map<String, String> map = this.tl.get();
        return map != null && map.containsKey(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final Map<String, String> map = this.tl.get();
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return this.tl.get();
    }

    @Override
    public boolean isEmpty() {
        return MapUtils.isEmpty(this.tl.get());
    }

    @Override
    public String toString() {
        final Map<String, String> map = this.tl.get();
        return map == null ? "{}" : map.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Map<String, String> map = this.tl.get();
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TtlThreadContextMap)) {
            return false;
        }
        final TtlThreadContextMap other = (TtlThreadContextMap) obj;
        final Map<String, String> map = this.tl.get();
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        if (map == null) {
            if (otherMap != null) {
                return false;
            }
        } else if (!map.equals(otherMap)) {
            return false;
        }
        return true;
    }
}
