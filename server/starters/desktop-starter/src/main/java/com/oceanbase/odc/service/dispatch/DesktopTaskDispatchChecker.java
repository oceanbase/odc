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
package com.oceanbase.odc.service.dispatch;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.NonNull;

/**
 * {@link DesktopTaskDispatchChecker}
 *
 * @author yh263208
 * @date 2022-03-29 16:27
 * @since ODC_release_3.3.0
 * @see TaskDispatchChecker
 */
@Component
@Profile("clientMode")
public class DesktopTaskDispatchChecker implements TaskDispatchChecker {

    @Override
    public boolean isThisMachine(@NonNull ExecutorInfo info) {
        return true;
    }

    @Override
    public boolean isTaskEntityOnThisMachine(@NonNull TaskEntity taskEntity) {
        return true;
    }

    @Override
    public boolean isTaskEntitySubmitOnThisMachine(@NonNull TaskEntity taskEntity) {
        return true;
    }
}
