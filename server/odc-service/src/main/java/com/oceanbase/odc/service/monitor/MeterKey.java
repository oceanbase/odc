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
package com.oceanbase.odc.service.monitor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.micrometer.core.instrument.Tag;
import lombok.Value;

@Value
public class MeterKey {
    MeterName meterName;
    Iterable<Tag> tags;
    String uniqueKey;
    Boolean needRemove;

    public static MeterKey ofMeter(MeterName meter) {
        return new MeterKey(meter, Collections.emptyList(), null, false);
    }

    public static MeterKey ofMeter(MeterName meter, Tag... tags) {
        return new MeterKey(meter, Arrays.asList(tags), null, false);
    }

    public static MeterKey ofNeedRemoveMeter(MeterName meter, Tag... tags) {
        return new MeterKey(meter, Arrays.asList(tags), null, true);
    }

    public static MeterKey ofMeter(MeterName meter, String uniqueKey, Tag... tags) {
        return new MeterKey(meter, Arrays.asList(tags), uniqueKey, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MeterKey that = (MeterKey) o;
        if (!Objects.equals(meterName, that.meterName))
            return false;
        if (!Objects.equals(uniqueKey, that.uniqueKey)) {
            return false;
        }
        if (!Objects.equals(needRemove, that.needRemove)) {
            return false;
        }
        // Convert tags to sets for order-insensitive comparison
        Set<Tag> thisTagSet = toTagSet(this.tags);
        Set<Tag> thatTagSet = toTagSet(that.tags);

        return Objects.equals(thisTagSet, thatTagSet);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(meterName, uniqueKey, needRemove);
        Set<Tag> tagSet = toTagSet(tags);
        result = 31 * result + tagSet.hashCode();
        return result;
    }

    private Set<Tag> toTagSet(Iterable<Tag> tags) {
        Set<Tag> tagSet = new HashSet<>();
        if (tags != null) {
            for (Tag tag : tags) {
                tagSet.add(tag);
            }
        }
        return tagSet;
    }

    public static class Builder {
        MeterName meterName;
        Set<Tag> tags;
        String uniqueKey;
        Boolean needRemove;

        private Builder() {}

        public static Builder ofMeter(MeterName meter) {
            Builder builder = new Builder();
            builder.meterName = meter;
            builder.needRemove = false;
            builder.tags = new HashSet<>();
            return builder;
        }

        public Builder uniqueKey(String uniqueKey) {
            this.uniqueKey = uniqueKey;
            return this;
        }

        public Builder needRemove() {
            this.needRemove = true;
            return this;
        }

        public Builder addTag(String key, String value) {
            this.tags.add(Tag.of(key, value));
            return this;
        }

        public MeterKey build() {
            return new MeterKey(meterName, tags, uniqueKey, needRemove);
        }

    }

}
