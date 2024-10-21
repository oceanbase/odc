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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.Value;

@Value
public class MeterKey {
    MeterName meterName;
    Tags tags;

    public static MeterKey ofMeter(DefaultMeterName meter) {
        return new MeterKey(meter, Tags.empty());
    }

    public static MeterKey ofMeter(DefaultMeterName meter, Tag... tags) {
        return new MeterKey(meter, Tags.of(tags));
    }

    public static class Builder {
        MeterName meterName;
        Tags tags;

        private Builder() {}

        public static Builder ofMeter(MeterName meter) {
            Builder builder = new Builder();
            builder.meterName = meter;
            builder.tags = Tags.empty();
            return builder;
        }

        public Builder addTag(String key, String value) {
            this.tags.and(Tag.of(key, value));
            return this;
        }

        public MeterKey build() {
            return new MeterKey(meterName, tags);
        }

    }

}
