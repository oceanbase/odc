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

package com.oceanbase.tools.sqlparser.util;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * {@link TimeoutTokenStream}
 *
 * @author yh263208
 * @date 2023-11-09 19:16
 * @since ODC_release_4.2.3
 */
public class TimeoutTokenStream extends CommonTokenStream {

    private final long timeoutTimestamp;

    public TimeoutTokenStream(TokenSource tokenSource, long timeoutMillis) {
        super(tokenSource);
        if (timeoutMillis <= 0) {
            this.timeoutTimestamp = Long.MAX_VALUE;
        } else {
            long target = System.currentTimeMillis() + timeoutMillis;
            this.timeoutTimestamp = target < 0 ? Long.MAX_VALUE : target;
        }
    }

    @Override
    public int LA(int i) {
        checkTimeout();
        return super.LA(i);
    }

    @Override
    public Token LT(int k) {
        checkTimeout();
        return super.LT(k);
    }

    private void checkTimeout() {
        if (System.currentTimeMillis() <= this.timeoutTimestamp) {
            return;
        }
        throw new ParseCancellationException("Timeout for parser, abort!");
    }

}
