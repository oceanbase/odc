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
package com.oceanbase.odc.common.dfa;

/**
 * {@link DfaStateTransfer}
 *
 * @author yh263208
 * @date 2024-09-04 20:32
 * @since ODC_release_4.3.2
 */
public interface DfaStateTransfer<STATE, INPUT> {

    STATE next();

    boolean matchesState(STATE state);

    boolean matchesInput(INPUT input);

}
