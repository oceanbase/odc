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

package com.oceanbase.odc.core.sql.split;

import java.util.Iterator;

/**
 * This interface is used to split SQL files into SQL statements iteratively. The
 * {@link OffsetString#getOffset()} is used to get the offset of the current SQL statement but may
 * be not accurate.
 * 
 * @author gaoda.xy
 * @date 2023/12/13 15:04
 */
public interface SqlStatementIterator extends Iterator<OffsetString> {

    /**
     * This method is used to get the current number of bytes of the SQL file stream that has been
     * traversed. The reason for placing it in the interface is that the exact value can only be
     * computed when it is processed inner the Iterator, whereas the value computed on the call side
     * based on the contents of the processed SQL is inaccurate. This is because there may be large
     * blank characters in the SQL which are removed during the SQL splitting process.
     *
     * @return bytes size that has been iterated
     */
    long iteratedBytes();

}
