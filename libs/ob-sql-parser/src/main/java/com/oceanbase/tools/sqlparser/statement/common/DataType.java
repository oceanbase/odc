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
package com.oceanbase.tools.sqlparser.statement.common;

import java.util.List;

import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * {@link DataType}
 *
 * @author yh263208
 * @date 2022-12-09 21:43
 * @since ODC_release_4.1.0
 * @see Statement
 */
public interface DataType extends Statement {

    String getName();

    List<String> getArguments();

}
