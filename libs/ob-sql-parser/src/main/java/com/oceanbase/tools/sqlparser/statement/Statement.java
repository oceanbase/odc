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
package com.oceanbase.tools.sqlparser.statement;

/**
 * {@link Statement}
 *
 * @author yh263208
 * @date 2022-11-24 20:07
 * @since ODC_release_4.1.0
 */
public interface Statement {
    /**
     * Text content of the {@link Statement}
     *
     * @return content
     */
    String getText();

    /**
     * Begin index of the {@link Statement}
     *
     * @return index
     */
    int getStart();

    /**
     * End index of the {@link Statement}
     *
     * @return index
     */
    int getStop();

    /**
     * Start line number of the {@link Statement}
     *
     * @return line number
     */
    int getLine();

    /**
     * column index in {@link Statement#getLine()}
     *
     * @return column index
     */
    int getCharPositionInLine();

}
